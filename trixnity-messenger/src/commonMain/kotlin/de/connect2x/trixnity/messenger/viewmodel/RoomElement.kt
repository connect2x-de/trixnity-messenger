package de.connect2x.trixnity.messenger.viewmodel

import net.folivo.trixnity.core.model.events.m.Presence

data class RoomNameElement(
    val roomName: String,
)

data class RoomHeaderElement(
    val roomName: String,
    val roomImageInitials: String,
    val roomImage: ByteArray?,
    val presence: Presence?,
    val isEncrypted: Boolean,
    val isPublic: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RoomHeaderElement

        if (roomName != other.roomName) return false
        if (roomImageInitials != other.roomImageInitials) return false
        if (roomImage != null) {
            if (other.roomImage == null) return false
            if (!roomImage.contentEquals(other.roomImage)) return false
        } else if (other.roomImage != null) return false
        if (presence != other.presence) return false
        if (isEncrypted != other.isEncrypted) return false
        if (isPublic != other.isPublic) return false

        return true
    }

    override fun hashCode(): Int {
        var result = roomName.hashCode()
        result = 31 * result + roomImageInitials.hashCode()
        result = 31 * result + (roomImage?.contentHashCode() ?: 0)
        result = 31 * result + (presence?.hashCode() ?: 0)
        result = 31 * result + isEncrypted.hashCode()
        result = 31 * result + isPublic.hashCode()
        return result
    }
}
