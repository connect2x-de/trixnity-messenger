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

expect fun platformGetSecretStringKeyModule(): Module
actual fun platformConvertSecretString(): Module = module {
    includes(platformGetSecretStringKeyModule())
    single<ConvertSecretString> {
        val getSecretStringKey = get<GetSecretStringKey>()
        object : ConvertSecretString {
            override suspend fun get(raw: String): SecretString {
                val secretStringKey = getSecretStringKey(SECRET_STRING_KEY_AES_HMAC_SHA2_ID, ::createNewKey)
                    ?: return SecretString.Unencrypted(raw)
                val encryptedStringSecret =
                    encryptAesHmacSha2(
                        raw.encodeToByteArray(),
                        secretStringKey,
                        SECRET_STRING_KEY_AES_HMAC_SHA2_ID
                    )
                return SecretString.AesHmacSha2(
                    iv = encryptedStringSecret.iv,
                    ciphertext = encryptedStringSecret.ciphertext,
                    mac = encryptedStringSecret.mac,
                )
            }

            override suspend fun get(secret: SecretString): String = when (secret) {
                is SecretString.Unencrypted -> secret.value
                is SecretString.AesHmacSha2 -> {
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
                    ).decodeToString()
                }
            }

            fun createNewKey() = SecureRandom.nextBytes(32)
        }
    }
}