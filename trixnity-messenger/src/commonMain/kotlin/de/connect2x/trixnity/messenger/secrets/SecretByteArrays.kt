package de.connect2x.trixnity.messenger.secrets

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.update
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.crypto.core.AesHmacSha2EncryptedData
import net.folivo.trixnity.crypto.core.decryptAesHmacSha2
import net.folivo.trixnity.crypto.core.encryptAesHmacSha2

private val log = KotlinLogging.logger {}

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

    @Deprecated("for backwards compatibility") // TODO function can be removed in future
    suspend fun getLegacy(secretByteArray: SecretByteArray): ByteArray
}

class SecretByteArraysImpl(
    private val settings: MatrixMessengerSettingsHolder,
    secretByteArrayKeyProviders: Lazy<List<SecretByteArrayKeyProvider>>,
) : SecretByteArrays {
    private val keySize = 32
    private val secretByteArrayKeyProviders: List<SecretByteArrayKeyProvider> by lazy {
        secretByteArrayKeyProviders.value.sortedWith(
            compareBy<SecretByteArrayKeyProvider> { it.level }.thenBy { it.id }
        )
    }

    override suspend fun set(id: String, raw: ByteArray?) {
        log.trace { "set SecretByteArray $id" }
        if (raw == null) {
            settings.update<MatrixMessengerSettingsBase> {
                it.copy(secretByteArrays = it.secretByteArrays - id)
            }
        } else {
            val secretByteArray = get(id, raw, getKey(keySize))
            settings.update<MatrixMessengerSettingsBase> {
                it.copy(secretByteArrays = it.secretByteArrays + (id to secretByteArray))
            }
        }
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
        val secretByteArray = settings.value.base.secretByteArrays[id]
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
        return rotateKeys(oldKey, changedProviderId, changedProviderRotate)
    }

    private suspend fun rotateKeys( // TODO this helper function with nullable params is for backwards compatibility and can be moved into the other without nullable
        oldKey: ByteArray?,
        changedProviderId: String?,
        changedProviderRotate: (suspend (oldExtra: JsonObject?, getOldInputKey: GetKey?, getNewInputKey: GetKey?) -> SecretByteArrayKeyProvider.RotateResult)?
    ) {
        log.debug { "rotateKeys (changedProviderId=$changedProviderId)" }
        val oldSecretByteArrayKeyInfos = settings.value.base.secretByteArrayKeyInfos
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
                    return@fold inputProviderId to inputRotateResult
                }
                newSecretByteArrayKeyInfos[outputProviderId] =
                    SecretByteArrayKeyInfo(inputProviderId, outputRotateResult.newExtra)
                log.debug { "rotateKeys next provider ($inputProviderId -> $outputProviderId)" }
                outputProviderId to outputRotateResult
            }.second?.let { it.getNewKey?.invoke(keySize) }

        val newSecretByteArrays = settings.value.base.secretByteArrays.mapValues { (id, secretByteArray) ->
            val byteArray = get(id, secretByteArray, oldKey)
            get(id, byteArray, newKey)
        }
        settings.update<MatrixMessengerSettingsBase> {
            it.copy(
                secretByteArrayKeyInfos = newSecretByteArrayKeyInfos,
                secretByteArrays = newSecretByteArrays
            )
        }
    }

    override suspend fun getInputKeyAndExtra(providerId: String): SecretByteArrays.GetInputKeyAndExtraResult {
        val secretByteArrayKeyInfos = settings.value.base.secretByteArrayKeyInfos
        val extra = settings.value.base.secretByteArrayKeyInfos[providerId]?.extra
        val orderedSecretByteArrayKeyProviders = getOrderedSecretByteArrayKeyProviders()
        val inputKey =
            if (orderedSecretByteArrayKeyProviders.any { it.id == providerId }) {
                orderedSecretByteArrayKeyProviders
                    .takeWhile { it.id != providerId }
                    .fold(null as GetKey?) { getInputKey, secretByteArrayKeyProvider ->
                        secretByteArrayKeyProvider.get(
                            secretByteArrayKeyInfos[secretByteArrayKeyProvider.id]?.extra,
                            getInputKey
                        )
                    }
            } else null
        return SecretByteArrays.GetInputKeyAndExtraResult(inputKey, extra)
    }

    @Deprecated("for backwards compatibility")
    override suspend fun getLegacy(secretByteArray: SecretByteArray): ByteArray {
        log.debug { "getLegacy SecretByteArray" }
        return when (secretByteArray) {
            is SecretByteArray.AesHmacSha2 -> {
                val getSecretByteArrayKey =
                    getLegacyKey()
                        ?: throw SecretByteArrayException("getLegacy: could not find secret key")
                decryptAesHmacSha2(
                    content = AesHmacSha2EncryptedData(
                        iv = secretByteArray.iv,
                        ciphertext = secretByteArray.ciphertext,
                        mac = secretByteArray.mac,
                    ),
                    key = getSecretByteArrayKey,
                    name = "secret"
                )
            }

            is SecretByteArray.Unencrypted -> secretByteArray.value
        }
    }

    private suspend fun getKey(size: Int): ByteArray? {
        val secretByteArrayKeyInfos = settings.value.base.secretByteArrayKeyInfos
        val orderedSecretByteArrayKeyProviders = getOrderedSecretByteArrayKeyProviders()
        log.debug { "getKey (size=$size, orderedSecretByteArrayKeyProviders=${orderedSecretByteArrayKeyProviders.map { it.id }})" }
        return orderedSecretByteArrayKeyProviders.fold(null as GetKey?) { getInputKey, secretByteArrayKeyProvider ->
            secretByteArrayKeyProvider.get(
                secretByteArrayKeyInfos[secretByteArrayKeyProvider.id]?.extra,
                getInputKey
            )
        }?.invoke(size)
    }

    private fun getOrderedSecretByteArrayKeyProviders(): List<SecretByteArrayKeyProvider> {
        val providerInfos = settings.value.base.secretByteArrayKeyInfos

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

    @Deprecated("for backwards compatibility")
    private suspend fun getLegacyKey(): ByteArray? {
        log.debug { "getLegacyKey" }
        val secretByteArrayKeyFromSettings = settings.value.base.secretByteArrayKey ?: return null
        val secretByteArrayKey = when (secretByteArrayKeyFromSettings) {
            is LegacySecretByteArrayKey.AesHmacSha2 -> {
                val secretByteArrayKeyKey =
                    secretByteArrayKeyProviders.reversed().firstNotNullOfOrNull { secretByteArrayKeyProvider ->
                        log.info { "try getLegacyKey from ${secretByteArrayKeyProvider.id}" }
                        secretByteArrayKeyProvider.getLegacy()
                    } ?: return null
                decryptAesHmacSha2(
                    content = AesHmacSha2EncryptedData(
                        iv = secretByteArrayKeyFromSettings.iv,
                        ciphertext = secretByteArrayKeyFromSettings.ciphertext,
                        mac = secretByteArrayKeyFromSettings.mac,
                    ),
                    key = secretByteArrayKeyKey,
                    name = "secret"
                )
            }

            is LegacySecretByteArrayKey.Unencrypted -> secretByteArrayKeyFromSettings.value
        }
        rotateKeys(secretByteArrayKey, null, null)
        return secretByteArrayKey
    }
}
