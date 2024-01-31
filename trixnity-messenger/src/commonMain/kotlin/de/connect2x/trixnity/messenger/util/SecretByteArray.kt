package de.connect2x.trixnity.messenger.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.folivo.trixnity.crypto.core.AesHmacSha2EncryptedData
import net.folivo.trixnity.crypto.core.decryptAesHmacSha2
import net.folivo.trixnity.crypto.core.encryptAesHmacSha2
import org.koin.dsl.module

@Serializable
sealed interface SecretByteArray {
    @Serializable
    @SerialName("aes-hmac-sha2")
    data class AesHmacSha2(
        val iv: String, // base64 encoded
        val ciphertext: String, // base64 encoded
        val mac: String // base64 encoded
    ) : SecretByteArray
}

interface ConvertSecretByteArray {
    suspend operator fun invoke(raw: ByteArray): SecretByteArray
    suspend operator fun invoke(secret: SecretByteArray): ByteArray
}

fun convertSecretByteArrayModule() = module {
    single<ConvertSecretByteArray> {
        val getSecretByteArrayKey = get<GetSecretByteArrayKey>()
        object : ConvertSecretByteArray {
            override suspend operator fun invoke(raw: ByteArray): SecretByteArray {
                val secretByteArrayKey = getSecretByteArrayKey(32)
                val encryptedStringSecret =
                    encryptAesHmacSha2(
                        content = raw,
                        key = secretByteArrayKey,
                        name = "secret"
                    )
                return SecretByteArray.AesHmacSha2(
                    iv = encryptedStringSecret.iv,
                    ciphertext = encryptedStringSecret.ciphertext,
                    mac = encryptedStringSecret.mac,
                )
            }

            override suspend operator fun invoke(secret: SecretByteArray): ByteArray =
                when (secret) {
                    is SecretByteArray.AesHmacSha2 -> {
                        val secretByteArrayKey = getSecretByteArrayKey(32)
                        decryptAesHmacSha2(
                            content = AesHmacSha2EncryptedData(
                                iv = secret.iv,
                                ciphertext = secret.ciphertext,
                                mac = secret.mac,
                            ),
                            key = secretByteArrayKey,
                            name = "secret"
                        )
                    }
                }
        }
    }
}