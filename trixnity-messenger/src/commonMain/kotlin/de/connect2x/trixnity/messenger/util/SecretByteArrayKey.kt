package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.folivo.trixnity.crypto.core.AesHmacSha2EncryptedData
import net.folivo.trixnity.crypto.core.SecureRandom
import net.folivo.trixnity.crypto.core.decryptAesHmacSha2
import net.folivo.trixnity.crypto.core.encryptAesHmacSha2
import org.koin.core.module.Module

private val log = KotlinLogging.logger { }

@Serializable
sealed interface SecretByteArrayKey {
    @Serializable
    @SerialName("aes-hmac-sha2")
    data class AesHmacSha2(
        val iv: String, // base64 encoded
        val ciphertext: String, // base64 encoded
        val mac: String // base64 encoded
    ) : SecretByteArrayKey

    /**
     * This is only needed when there is no secure way to store the key. This is not secure at all, but it allows us to
     * make it secure in future (for example when keyring support for linux is added).
     */
    @Serializable
    @SerialName("unencrypted")
    data class Unencrypted(val value: @Serializable(ByteArrayBase64Serializer::class) ByteArray) : SecretByteArrayKey {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Unencrypted

            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }
    }
}

interface GetSecretByteArrayKey {
    /**
     * @return null when not possible to create a key on this platform
     */
    suspend operator fun invoke(sizeOnCreate: Int): ByteArray
}

abstract class GetSecretByteArrayKeyBase(
    private val settings: MatrixMessengerSettingsHolder,
) : GetSecretByteArrayKey {
    protected abstract suspend fun getSecretByteArrayKeyKey(sizeOnCreate: Int): ByteArray?

    protected fun getSecretByteArrayKeyFromSettings() = settings.value.secretByteArrayKey
    protected suspend fun setSecretByteArrayKeyInSettings(secretByteArrayKey: SecretByteArrayKey?) =
        settings.update { it.copy(secretByteArrayKey = secretByteArrayKey) }

    private val mutex = Mutex()
    override suspend fun invoke(sizeOnCreate: Int): ByteArray = mutex.withLock {
        val existing = getSecretByteArrayKeyFromSettings()
        if (existing != null) convert(existing, getSecretByteArrayKeyKey(32))
        else {
            log.debug { "there is SecretByteArrayKey yet, generate new one" }
            val newKey = SecureRandom.nextBytes(sizeOnCreate)
            val secretByteArrayKey = convert(newKey, getSecretByteArrayKeyKey(32))
            setSecretByteArrayKeyInSettings(secretByteArrayKey)
            newKey
        }
    }

    protected suspend fun convert(
        secretByteArrayKey: SecretByteArrayKey,
        secretByteArrayKeyKey: ByteArray?,
    ): ByteArray =
        when (secretByteArrayKey) {
            is SecretByteArrayKey.AesHmacSha2 -> {
                requireNotNull(secretByteArrayKeyKey) { "could not find key for SecretByteArrayKey" }
                decryptAesHmacSha2(
                    content = AesHmacSha2EncryptedData(
                        iv = secretByteArrayKey.iv,
                        ciphertext = secretByteArrayKey.ciphertext,
                        mac = secretByteArrayKey.mac,
                    ),
                    key = secretByteArrayKeyKey,
                    name = "secret"
                )
            }

            is SecretByteArrayKey.Unencrypted -> secretByteArrayKey.value
        }

    protected suspend fun convert(
        raw: ByteArray,
        secretByteArrayKeyKey: ByteArray?,
    ): SecretByteArrayKey =
        if (secretByteArrayKeyKey != null) {
            val encryptedStringSecret =
                encryptAesHmacSha2(
                    content = raw,
                    key = secretByteArrayKeyKey,
                    name = "secret"
                )
            SecretByteArrayKey.AesHmacSha2(
                iv = encryptedStringSecret.iv,
                ciphertext = encryptedStringSecret.ciphertext,
                mac = encryptedStringSecret.mac,
            )
        } else SecretByteArrayKey.Unencrypted(raw)
}

expect fun platformGetSecretByteArrayKey(): Module
