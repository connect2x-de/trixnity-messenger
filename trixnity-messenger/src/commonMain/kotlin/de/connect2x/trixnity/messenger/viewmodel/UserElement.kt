package de.connect2x.trixnity.messenger.viewmodel

import kotlinx.coroutines.flow.firstOrNull
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId

data class UserInfoElement(
    val name: String,
    val initials: String? = null,
    val image: ByteArray? = null,
    val userId: UserId? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UserInfoElement

        if (name != other.name) return false
        if (initials != other.initials) return false
        if (image != null) {
            if (other.image == null) return false
            if (!image.contentEquals(other.image)) return false
        } else if (other.image != null) return false
        if (userId != other.userId) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (initials?.hashCode() ?: 0)
        result = 31 * result + (image?.contentHashCode() ?: 0)
        result = 31 * result + (userId?.hashCode() ?: 0)
        return result
    }
}

suspend fun UserId.toUserInfoElement(matrixClient: MatrixClient, roomId: RoomId): UserInfoElement {
    return UserInfoElement(
        name = matrixClient.user.getById(roomId, this).firstOrNull()?.name ?: this.localpart,
        userId = this
    )
}
