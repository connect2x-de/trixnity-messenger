@file:OptIn(MSC4143::class)

package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import de.connect2x.livekitnity.api.e2ee.KeyProvider
import de.connect2x.trixnity.client.rtc.PerMemberRtcSlotMembershipEncryptionInfo
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.rtc.RtcEncryptionKeyEventContent
import de.connect2x.trixnity.core.model.events.m.rtc.RtcMemberId
import de.connect2x.trixnity.core.serialization.canonicalJsonString
import de.connect2x.trixnity.crypto.core.Sha256
import de.connect2x.trixnity.utils.decodeUnpaddedBase64Bytes
import de.connect2x.trixnity.utils.encodeUnpaddedBase64
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

internal class MatrixRtcKeyProvider(private val keyProvider: KeyProvider) {
    /** Matrix keys are Base64-encoded binary, unlike BaseKeyProvider's UTF-8 strings. */
    suspend fun setKey(key: String, participantId: String?, keyIndex: Int?) {
        val identity = requireNotNull(participantId) { "Missing MatrixRTC participant identity" }
        val index = keyIndex ?: 0
        require(index in 0..255) { "Invalid MatrixRTC key index" }
        val bytes = key.decodeUnpaddedBase64Bytes()
        // Matrix rtc says 32 bytes but Element Call uses 16
        require(bytes.size >= 16) { "MatrixRTC LiveKit key is too short" }
        keyProvider.setKey(bytes, identity, index)
    }

    suspend fun update(info: PerMemberRtcSlotMembershipEncryptionInfo, userId: UserId) {
        val content = info.ownActiveKey
        val memberId = requireNotNull(content.memberId ?: content.member?.id) { "Missing local MatrixRTC member ID" }
        val localIdentity = matrixRtcParticipantIdentity(userId, memberId)
        setKey(content, localIdentity)
        info.receivedKeys.forEach { (member, contents) ->
            val identity = matrixRtcParticipantIdentity(member.first, member.second)
            if (identity != localIdentity) {
                contents.forEach { remote ->
                    try {
                        setKey(remote, identity)
                    } catch (_: IllegalArgumentException) {
                        // Ignore malformed remote keys without interrupting local encryption.
                    }
                }
            }
        }
    }

    private suspend fun setKey(content: RtcEncryptionKeyEventContent, identity: String) {
        val key = requireNotNull(content.mediaKey ?: content.keys) { "Missing MatrixRTC media key" }
        require(key.index in 0..255) { "Invalid MatrixRTC key index: ${key.index}" }
        setKey(key.key, identity, key.index.toInt())
    }
}

internal fun matrixRtcParticipantIdentity(userId: UserId, memberId: RtcMemberId): String {
    val canonical = canonicalJsonString(JsonArray(listOf(JsonPrimitive(userId.full), JsonPrimitive(memberId.value))))
    return Sha256().use { hasher ->
        hasher.update(canonical.encodeToByteArray())
        hasher.digest().encodeUnpaddedBase64()
    }
}
