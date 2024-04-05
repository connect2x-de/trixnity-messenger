package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.utils.toByteArray

data class RoomInfoElement(
    val roomName: String,
    var roomId: RoomId,
    val roomImageInitials: String,
    val roomImage: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RoomInfoElement

        if (roomName != other.roomName) return false
        if (roomId != other.roomId) return false
        if (roomImageInitials != other.roomImageInitials) return false
        if (roomImage != null) {
            if (other.roomImage == null) return false
            if (!roomImage.contentEquals(other.roomImage)) return false
        } else if (other.roomImage != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = roomName.hashCode()
        result = 31 * result + roomId.hashCode()
        result = 31 * result + roomImageInitials.hashCode()
        result = 31 * result + (roomImage?.contentHashCode() ?: 0)
        return result
    }
}

suspend fun Room.toRoomInfoElement(matrixClient: MatrixClient): RoomInfoElement {
    val name = this.name?.explicitName ?: this.roomId.let { "${it.localpart}:${it.domain}" }
    return RoomInfoElement(
        roomName = name,
        roomId = this.roomId,
        roomImageInitials = Initials.compute(name),
        roomImage = this.avatarUrl?.let {
            matrixClient.media.getMedia(it).getOrNull()?.toByteArray()
        }
    )
}
