package de.connect2x.trixnity.messenger.util

import io.ktor.util.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.koin.core.module.Module

@Serializable
sealed interface SecretByteArray {
    @Serializable
    @SerialName("unencrypted")
    data class Unencrypted(val value: @Serializable(ByteArrayBase64Serializer::class) ByteArray) : SecretByteArray {
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

    @Serializable
    @SerialName("aes-hmac-sha2")
    data class AesHmacSha2(
        val iv: String, // base64 encoded
        val ciphertext: String, // base64 encoded
        val mac: String // base64 encoded
    ) : SecretByteArray
}

class ByteArrayBase64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ByteArrayBase64Serializer")

    override fun deserialize(decoder: Decoder): ByteArray {
        return decoder.decodeString().decodeBase64Bytes()
    }

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(value.encodeBase64())
    }
}

interface ConvertSecretByteArray {
    suspend operator fun invoke(raw: ByteArray): SecretByteArray
    suspend operator fun invoke(secret: SecretByteArray): ByteArray
}

expect fun platformConvertSecretByteArray(): Module