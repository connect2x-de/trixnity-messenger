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

    /**
     * This may suspend for a while when e.g. user interaction is needed.
     *
     * @return The key or null, when provider disabled.
     */
    suspend fun get(extra: JsonObject?, getInputKey: GetKey?): GetKey?

    data class RotateResult(
        val getOldKey: GetKey?,
        val getNewKey: GetKey?,
        val newExtra: JsonObject?,
    )

    suspend fun rotate(oldExtra: JsonObject?, getOldInputKey: GetKey?, getNewInputKey: GetKey?): RotateResult

    @Deprecated("for backwards compatibility") // TODO can be removed in future
    suspend fun getLegacy(): ByteArray?
}
