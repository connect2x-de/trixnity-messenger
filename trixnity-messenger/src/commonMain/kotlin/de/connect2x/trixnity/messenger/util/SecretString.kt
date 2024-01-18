package de.connect2x.trixnity.messenger.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.module.Module

@Serializable
sealed interface SecretString {
    @Serializable
    @SerialName("unencrypted")
    data class Unencrypted(val value: String) : SecretString

    @Serializable
    @SerialName("aes-hmac-sha2")
    data class AesHmacSha2(
        val iv: String, // base64 encoded
        val ciphertext: String, // base64 encoded
        val mac: String // base64 encoded
    ) : SecretString
}

interface ConvertSecretString {
    suspend operator fun invoke(raw: String): SecretString
    suspend operator fun invoke(secret: SecretString): String
}

expect fun platformConvertSecretString(): Module