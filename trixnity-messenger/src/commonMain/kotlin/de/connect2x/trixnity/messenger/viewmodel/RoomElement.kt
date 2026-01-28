package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import kotlinx.coroutines.CoroutineScope
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.core.model.RoomId

data class RoomInfoElement(
    val name: String,
    var roomId: RoomId,
    val roomImageInitials: String,
    val roomImage: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RoomInfoElement

        if (name != other.name) return false
        if (roomId != other.roomId) return false
        if (roomImageInitials != other.roomImageInitials) return false
        if (roomImage != null) {
            if (other.roomImage == null) return false
            if (!roomImage.contentEquals(other.roomImage)) return false
        } else if (other.roomImage != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + roomId.hashCode()
        result = 31 * result + roomImageInitials.hashCode()
        result = 31 * result + (roomImage?.contentHashCode() ?: 0)
        return result
    }
}

suspend fun Room.toRoomInfoElement(
    coroutineScope: CoroutineScope,
    initials: Initials,
    matrixClient: MatrixClient,
    name: String,
    maxMediaSizeInMemory: Long,
): RoomInfoElement {
    return RoomInfoElement(
        name = name,
        roomId = roomId,
        roomImageInitials = initials.compute(name),
        roomImage =
            this.avatarUrl?.let {
                matrixClient.media.getMedia(it).getOrNull()?.toByteArray(coroutineScope, maxSize = maxMediaSizeInMemory)
            }
    )
}
