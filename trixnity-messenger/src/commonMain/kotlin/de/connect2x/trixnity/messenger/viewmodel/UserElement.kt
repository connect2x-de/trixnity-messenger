package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import korlibs.io.net.http.createHttpClient
import kotlinx.coroutines.flow.first
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.store.originalName
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.utils.toByteReadChannel

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

suspend fun RoomUser.toUserInfoElement(matrixClient: MatrixClient): UserInfoElement =
    UserInfoElement(
        name = this.originalName ?: this.name,
        userId = this.userId,
        initials = Initials.compute(this.originalName ?: this.name),
        image = this.avatarUrl?.let {
            matrixClient.media.getMedia(it).getOrNull()?.first()
        }
    )

