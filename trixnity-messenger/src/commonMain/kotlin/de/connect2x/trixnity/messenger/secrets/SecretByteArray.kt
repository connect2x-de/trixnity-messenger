package de.connect2x.trixnity.messenger.secrets

import de.connect2x.trixnity.messenger.util.ByteArrayBase64Serializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SecretByteArray {
    @Serializable
    @SerialName("aes-hmac-sha2")
    data class AesHmacSha2(
        val iv: String, // base64 encoded
        val ciphertext: String, // base64 encoded
        val mac: String // base64 encoded
    ) : SecretByteArray

    /**
     * This is only needed when there is no secure way to store the key. This is not secure at all, but it allows us to
     * make it secure in future (for example when keyring support for linux is added).
     */
    @Serializable
    @SerialName("unencrypted")
    data class Unencrypted(
        val value: @Serializable(ByteArrayBase64Serializer::class) ByteArray
    ) : SecretByteArray {
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
