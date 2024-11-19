package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId

private val log = KotlinLogging.logger {}

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

suspend fun Room.toRoomInfoElement(matrixClient: MatrixClient, name: String, maxAvatarSize: Long): RoomInfoElement {
    return RoomInfoElement(
        name = name,
        roomId = roomId,
        roomImageInitials = Initials.compute(name),
        roomImage =
            this.avatarUrl?.let {
                matrixClient.media.getMedia(it).getOrNull()?.limitedByteArrayOrNull(maxAvatarSize) {
                    log.error { "Room image for room $roomId exceeds preview size limits, so it's not displayed" }
                }
            }
    )
}
