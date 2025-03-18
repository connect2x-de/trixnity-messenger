package de.connect2x.trixnity.messenger.secrets

import kotlinx.serialization.json.JsonObject

interface SecretByteArrayKeyProvider {
    /**
     * Unique id to identify, where this key is needed.
     */
    val id: String

    /**
     * The position in the key provider chain.
     */
    val level: Int

    data class GetResult(
        val getKey: GetKey,
        val extra: JsonObject?,
    )

    /**
     * This may suspend for a while when e.g. user interaction is needed.
     *
     * @return The key or null, when provider disabled.
     */
    suspend fun get(extra: JsonObject?, getInputKey: GetKey?): GetResult?

    @Deprecated("for backwards compatibility") // TODO can be removed in future
    suspend fun getLegacy(): ByteArray?
}
