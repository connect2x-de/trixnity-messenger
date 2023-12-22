package de.connect2x.trixnity.messenger.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import org.koin.core.module.Module

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface SecretString {
    @SerialName("unencrypted")
    data class Unencrypted(val value: String) : SecretString

    @SerialName("aes-hmac-sha2")
    data class AesHmacSha2(
        val iv: String, // base64 encoded
        val ciphertext: String, // base64 encoded
        val mac: String // base64 encoded
    ) : SecretString
}

interface ConvertSecretString {
    suspend fun get(raw: String): SecretString
    suspend fun get(secret: SecretString): String
}

expect fun platformConvertSecretString(): Module