package de.connect2x.trixnity.messenger.secrets

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.update
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.crypto.core.AesHmacSha2EncryptedData
import net.folivo.trixnity.crypto.core.decryptAesHmacSha2
import net.folivo.trixnity.crypto.core.encryptAesHmacSha2
import org.koin.core.Koin

private val log = KotlinLogging.logger {}

interface SecretByteArrays {
    suspend fun set(id: String, raw: ByteArray?)
    suspend fun get(id: String): ByteArray?

    suspend fun rotateKeys(
        changedProviderId: String,
        changedProviderGet: suspend (extra: JsonObject?, getInputKey: GetKey?) -> SecretByteArrayKeyProvider.GetResult?
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
    private val di: Koin,
) : SecretByteArrays {
    private val keySize = 32
    private val secretByteArrayKeyProviders: List<SecretByteArrayKeyProvider> by lazy {
        di.getAll<SecretByteArrayKeyProvider>().sortedWith(
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
                ?: throw SecretByteArrayException("secret $id requires a key, but none was found")
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
        changedProviderGet: suspend (extra: JsonObject?, getInputKey: GetKey?) -> SecretByteArrayKeyProvider.GetResult?
    ) {
        val oldKey = getKey(keySize)
        return rotateKeys(oldKey, changedProviderId, changedProviderGet)
    }

    private suspend fun rotateKeys( // TODO this helper function with nullable params is for backwards compatibility and can be moved into the other without nullable
        oldKey: ByteArray?,
        changedProviderId: String?,
        changedProviderGet: (suspend (extra: JsonObject?, getInputKey: GetKey?) -> SecretByteArrayKeyProvider.GetResult?)?
    ) {
        val oldSecretByteArrayKeyInfos = settings.value.base.secretByteArrayKeyInfos
        val newSecretByteArrayKeyInfos = mutableMapOf<String, SecretByteArrayKeyInfo>()
        val newKey =
            secretByteArrayKeyProviders.fold((null to null) as Pair<String?, GetKey?>) { (inputProviderId, getInputKey), secretByteArrayKeyProvider ->
                val outputProviderId = secretByteArrayKeyProvider.id
                val getResult =
                    if (changedProviderGet != null && outputProviderId == changedProviderId) {
                        changedProviderGet(oldSecretByteArrayKeyInfos[outputProviderId]?.extra, getInputKey)
                    } else {
                        secretByteArrayKeyProvider.get(oldSecretByteArrayKeyInfos[outputProviderId]?.extra, getInputKey)
                    }
                if (getResult == null) {
                    log.info { "skip key provider $outputProviderId" }
                    return@fold inputProviderId to getInputKey
                }
                newSecretByteArrayKeyInfos[outputProviderId] = SecretByteArrayKeyInfo(inputProviderId, getResult.extra)
                outputProviderId to getResult.getKey
            }.second
                ?.let { it(keySize) }

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
                        )?.getKey
                    }
            } else null
        return SecretByteArrays.GetInputKeyAndExtraResult(inputKey, extra)
    }

    @Deprecated("for backwards compatibility")
    override suspend fun getLegacy(secretByteArray: SecretByteArray): ByteArray {
        log.info { "getLegacy SecretByteArray" }
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
        return orderedSecretByteArrayKeyProviders.fold(null as GetKey?) { getInputKey, secretByteArrayKeyProvider ->
            secretByteArrayKeyProvider.get(
                secretByteArrayKeyInfos[secretByteArrayKeyProvider.id]?.extra,
                getInputKey
            )?.getKey
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
                log.error { "could not find provider for id: $id" }
                return emptyList()
            }
            provider
        }
    }

    @Deprecated("for backwards compatibility")
    private suspend fun getLegacyKey(): ByteArray? {
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
