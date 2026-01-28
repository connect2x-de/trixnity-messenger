package de.connect2x.trixnity.messenger.secrets

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.SecretByteArraySettings
import de.connect2x.trixnity.messenger.settings.SettingsJson
import de.connect2x.trixnity.messenger.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import de.connect2x.trixnity.core.serialization.canonicalJsonString
import de.connect2x.trixnity.crypto.core.AesHmacSha2EncryptedData
import de.connect2x.trixnity.crypto.core.decryptAesHmacSha2
import de.connect2x.trixnity.crypto.core.encryptAesHmacSha2
import de.connect2x.trixnity.crypto.core.hmacSha256

interface SecretByteArrays {
    suspend fun set(id: String, raw: ByteArray?)
    suspend fun get(id: String): ByteArray?

    suspend fun rotateKeys(
        changedProviderId: String,
        changedProviderRotate: suspend (oldExtra: JsonObject?, getOldInputKey: GetKey?, getNewInputKey: GetKey?) -> SecretByteArrayKeyProvider.RotateResult
    )

    data class GetInputKeyAndExtraResult(
        val getInputKey: GetKey?,
        val extra: JsonObject?
    )

    suspend fun getInputKeyAndExtra(providerId: String): GetInputKeyAndExtraResult
}

class SecretByteArraysImpl(
    private val settings: MatrixMessengerSettingsHolder,
    secretByteArrayKeyProviders: Lazy<List<SecretByteArrayKeyProvider>>,
) : SecretByteArrays {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.secrets.SecretByteArraysImpl")
    }

    private val keySize = 32
    private val secretByteArrayKeyProviders: List<SecretByteArrayKeyProvider> by lazy {
        secretByteArrayKeyProviders.value.sortedWith(
            compareBy<SecretByteArrayKeyProvider> { it.level }.thenBy { it.id }
        )
    }

    private val setMutex = Mutex() // prevent concurrent settings update
    override suspend fun set(id: String, raw: ByteArray?) = setMutex.withLock {
        log.trace { "set SecretByteArray $id" }
        val secretByteArraysSettings = getSettingsOrInitialize()
        val key = getKey(keySize)
        val newSettings =
            if (raw == null) {
                SecretByteArraySettings(
                    secrets = secretByteArraysSettings.secrets.orEmpty() - id,
                    keyInfo = secretByteArraysSettings.keyInfo.orEmpty(),
                    key = key
                )
            } else {
                val secretByteArray = get(id, raw, key)
                SecretByteArraySettings(
                    secrets = secretByteArraysSettings.secrets.orEmpty() + (id to secretByteArray),
                    keyInfo = secretByteArraysSettings.keyInfo.orEmpty(),
                    key = key
                )
            }
        settings.update<SecretByteArraySettings> { newSettings }
    }

    private suspend fun get(id: String, raw: ByteArray, secretByteArrayKey: ByteArray?): SecretByteArray {
        val secretByteArray =
            if (secretByteArrayKey != null) {
                val encryptedSecret =
                    encryptAesHmacSha2(
                        content = raw,
                        key = secretByteArrayKey,
                        name = id
                    )
                SecretByteArray.AesHmacSha2(
                    iv = encryptedSecret.iv,
                    ciphertext = encryptedSecret.ciphertext,
                    mac = encryptedSecret.mac,
                )
            } else {
                SecretByteArray.Unencrypted(value = raw)
            }
        return secretByteArray
    }

    override suspend fun get(id: String): ByteArray? {
        log.trace { "get SecretByteArray $id" }
        val secretByteArraySettings = getSettingsOrInitialize()
        val secretByteArray = secretByteArraySettings.secrets?.get(id)
        return if (secretByteArray == null) return null
        else {
            val secretByteArrayKey = getKey(keySize)
            get(id, secretByteArray, secretByteArrayKey)
        }
    }

    private suspend fun get(id: String, secretByteArray: SecretByteArray, secretByteArrayKey: ByteArray?): ByteArray {
        return when (secretByteArray) {
            is SecretByteArray.AesHmacSha2 -> {
                decryptAesHmacSha2(
                    content = AesHmacSha2EncryptedData(
                        iv = secretByteArray.iv,
                        ciphertext = secretByteArray.ciphertext,
                        mac = secretByteArray.mac,
                    ),
                    key = secretByteArrayKey
                        ?: throw SecretByteArrayException("secret $id requires a key, but none was found"),
                    name = id
                )
            }

            is SecretByteArray.Unencrypted -> secretByteArray.value
        }
    }

    override suspend fun rotateKeys(
        changedProviderId: String,
        changedProviderRotate: suspend (oldExtra: JsonObject?, getOldInputKey: GetKey?, getNewInputKey: GetKey?) -> SecretByteArrayKeyProvider.RotateResult
    ) {
        val oldKey = getKey(keySize)
        rotateKeys(oldKey, changedProviderId, changedProviderRotate)
    }

    private suspend fun getSettingsOrInitialize(): SecretByteArraySettings {
        return settings.value.secretByteArrays.takeIf { it.secrets != null && it.keyInfo != null }
            ?: rotateKeys(null, null, null)
    }

    private val rotateKeysLock = Mutex()
    private suspend fun rotateKeys(
        oldKey: ByteArray?,
        changedProviderId: String?,
        changedProviderRotate: (suspend (oldExtra: JsonObject?, getOldInputKey: GetKey?, getNewInputKey: GetKey?) -> SecretByteArrayKeyProvider.RotateResult)?
    ): SecretByteArraySettings = rotateKeysLock.withLock {
        log.debug { "rotateKeys (changedProviderId=$changedProviderId)" }
        val secretByteArraySettings = settings.value.secretByteArrays
        val oldSecretByteArrayKeyInfos = secretByteArraySettings?.keyInfo.orEmpty()
        val newSecretByteArrayKeyInfos = mutableMapOf<String, SecretByteArrayKeyInfo>()
        val newKey =
            secretByteArrayKeyProviders.fold((null to null) as Pair<String?, SecretByteArrayKeyProvider.RotateResult?>) { (inputProviderId, inputRotateResult), secretByteArrayKeyProvider ->
                val outputProviderId = secretByteArrayKeyProvider.id
                val outputRotateResult =
                    if (changedProviderRotate != null && outputProviderId == changedProviderId) {
                        changedProviderRotate(
                            oldSecretByteArrayKeyInfos[outputProviderId]?.extra,
                            inputRotateResult?.getOldKey,
                            inputRotateResult?.getNewKey
                        )
                    } else {
                        secretByteArrayKeyProvider.rotate(
                            oldSecretByteArrayKeyInfos[outputProviderId]?.extra,
                            inputRotateResult?.getOldKey,
                            inputRotateResult?.getNewKey
                        )
                    }
                if (outputRotateResult.getNewKey == null) {
                    log.debug { "rotateKeys skip key provider $outputProviderId" }
                    inputProviderId to inputRotateResult
                } else {
                    newSecretByteArrayKeyInfos[outputProviderId] =
                        SecretByteArrayKeyInfo(inputProviderId, outputRotateResult.newExtra)
                    log.debug { "rotateKeys next provider ($inputProviderId -> $outputProviderId)" }
                    outputProviderId to outputRotateResult
                }
            }.second?.let { it.getNewKey?.invoke(keySize) }

        val newSecretByteArrays = secretByteArraySettings?.secrets.orEmpty()
            .mapValues { (id, secretByteArray) ->
                val byteArray = get(id, secretByteArray, oldKey)
                get(id, byteArray, newKey)
            }
        val newSettings =
            SecretByteArraySettings(
                secrets = newSecretByteArrays,
                keyInfo = newSecretByteArrayKeyInfos,
                key = newKey
            )
        settings.update<SecretByteArraySettings> { newSettings }
        newSettings
    }

    override suspend fun getInputKeyAndExtra(providerId: String): SecretByteArrays.GetInputKeyAndExtraResult {
        val secretByteArraySettings = getSettingsOrInitialize()
        val secretByteArrayKeyInfos = secretByteArraySettings.keyInfo
        val extra = secretByteArrayKeyInfos?.get(providerId)?.extra
        val orderedSecretByteArrayKeyProviders = getOrderedSecretByteArrayKeyProviders(secretByteArraySettings)
        val inputKey =
            if (orderedSecretByteArrayKeyProviders.any { it.id == providerId }) {
                orderedSecretByteArrayKeyProviders
                    .takeWhile { it.id != providerId }
                    .fold(null as GetKey?) { getInputKey, secretByteArrayKeyProvider ->
                        secretByteArrayKeyProvider.get(
                            extra = secretByteArrayKeyInfos?.get(secretByteArrayKeyProvider.id)?.extra,
                            getInputKey = getInputKey
                        )
                    }
            } else null
        return SecretByteArrays.GetInputKeyAndExtraResult(inputKey, extra)
    }

    private suspend fun getKey(size: Int): ByteArray? {
        val secretByteArraySettings = getSettingsOrInitialize()
        val secretByteArrayKeyInfos = secretByteArraySettings.keyInfo
        val orderedSecretByteArrayKeyProviders = getOrderedSecretByteArrayKeyProviders(secretByteArraySettings)
        log.debug { "getKey (size=$size, orderedSecretByteArrayKeyProviders=${orderedSecretByteArrayKeyProviders.map { it.id }})" }
        val key = orderedSecretByteArrayKeyProviders.fold(null as GetKey?) { getInputKey, secretByteArrayKeyProvider ->
            secretByteArrayKeyProvider.get(
                secretByteArrayKeyInfos?.get(secretByteArrayKeyProvider.id)?.extra,
                getInputKey
            )
        }?.invoke(size)
        secretByteArraySettings.checkIntegrity(key)
        return key
    }

    private fun getOrderedSecretByteArrayKeyProviders(secretByteArraySettings: SecretByteArraySettings?): List<SecretByteArrayKeyProvider> {
        val providerInfos = secretByteArraySettings?.keyInfo.orEmpty()

        val rootProviderId = providerInfos.entries.find { it.value.dependsOn == null }?.key
            ?: return emptyList()

        val orderedProviderIds = mutableListOf<String>()
        var nextProviderId: String? = rootProviderId
        while (nextProviderId != null) {
            orderedProviderIds.add(nextProviderId)
            nextProviderId = providerInfos.entries.find { it.value.dependsOn == nextProviderId }?.key
        }

        return orderedProviderIds.map { id ->
            val provider = secretByteArrayKeyProviders.find { it.id == id }
            if (provider == null) {
                throw SecretByteArrayException("could not find provider for id: $id")
            }
            provider
        }
    }
}

suspend fun SecretByteArraySettings(
    secrets: Map<String, SecretByteArray>,
    keyInfo: Map<String, SecretByteArrayKeyInfo>,
    key: ByteArray?
) = SecretByteArraySettings(
    secrets = secrets,
    keyInfo = keyInfo,
    mac = getMac(secrets, keyInfo, key),
)

suspend fun SecretByteArraySettings.checkIntegrity(key: ByteArray?) {
    val calculatedMac = getMac(secrets.orEmpty(), keyInfo.orEmpty(), key)
    if (!mac.contentEquals(calculatedMac))
        throw SecretByteArrayException("SecretByteArray integrity check failed")
}

private suspend fun getMac(
    secrets: Map<String, SecretByteArray>,
    keyInfo: Map<String, SecretByteArrayKeyInfo>,
    key: ByteArray?
): ByteArray? {
    if (key == null) return null
    val content = canonicalJsonString(
        JsonObject(buildMap {
            put("secrets", SettingsJson.encodeToJsonElement(secrets).jsonObject)
            put("keyInfo", SettingsJson.encodeToJsonElement(keyInfo).jsonObject)
        })
    ).encodeToByteArray()
    return hmacSha256(key, content)
}
