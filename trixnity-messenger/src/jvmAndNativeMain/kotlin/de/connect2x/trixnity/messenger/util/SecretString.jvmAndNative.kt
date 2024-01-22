package de.connect2x.trixnity.messenger.util

import net.folivo.trixnity.crypto.core.AesHmacSha2EncryptedData
import net.folivo.trixnity.crypto.core.SecureRandom
import net.folivo.trixnity.crypto.core.decryptAesHmacSha2
import net.folivo.trixnity.crypto.core.encryptAesHmacSha2
import org.koin.core.module.Module
import org.koin.dsl.module

private const val SECRET_STRING_KEY_AES_HMAC_SHA2_ID = "secret_string_key_aes_hmac_sha2"

fun interface GetSecretStringKey {
    suspend operator fun invoke(id: String, create: () -> ByteArray): ByteArray?
}

open class ConvertSecretByteArrayImpl(private val getSecretStringKey: GetSecretStringKey) : ConvertSecretByteArray {
    override suspend operator fun invoke(raw: ByteArray): SecretByteArray {
        val secretByteArrayKey = getSecretStringKey(SECRET_STRING_KEY_AES_HMAC_SHA2_ID, ::createNewKey)
            ?: return SecretByteArray.Unencrypted(raw)
        val encryptedStringSecret =
            encryptAesHmacSha2(
                raw,
                secretByteArrayKey,
                SECRET_STRING_KEY_AES_HMAC_SHA2_ID
            )
        return SecretByteArray.AesHmacSha2(
            iv = encryptedStringSecret.iv,
            ciphertext = encryptedStringSecret.ciphertext,
            mac = encryptedStringSecret.mac,
        )
    }

    override suspend operator fun invoke(secret: SecretByteArray): ByteArray = when (secret) {
        is SecretByteArray.Unencrypted -> secret.value
        is SecretByteArray.AesHmacSha2 -> {
            decryptAesHmacSha2(
                content = AesHmacSha2EncryptedData(
                    iv = secret.iv,
                    ciphertext = secret.ciphertext,
                    mac = secret.mac,
                ),
                key = checkNotNull(
                    getSecretStringKey(SECRET_STRING_KEY_AES_HMAC_SHA2_ID, ::createNewKey)
                ) { "could not retrieve StringSecretkey $SECRET_STRING_KEY_AES_HMAC_SHA2_ID" },
                name = SECRET_STRING_KEY_AES_HMAC_SHA2_ID
            )
        }
    }

    private fun createNewKey() = SecureRandom.nextBytes(32)
}

expect fun platformGetSecretStringKeyModule(): Module
actual fun platformConvertSecretByteArray(): Module = module {
    includes(platformGetSecretStringKeyModule())
    single<ConvertSecretByteArray> {
        ConvertSecretByteArrayImpl(get())
    }
}