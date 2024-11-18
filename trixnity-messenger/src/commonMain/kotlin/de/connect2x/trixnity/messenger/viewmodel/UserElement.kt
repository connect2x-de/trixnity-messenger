package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.core.model.UserId

private val log = KotlinLogging.logger { }

data class UserInfoElement(
    val name: String,
    val userId: UserId,
    val initials: String? = null,
    val image: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UserInfoElement

        if (name != other.name) return false
        if (userId != other.userId) return false
        if (initials != other.initials) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + (initials?.hashCode() ?: 0)
        result = 31 * result + (image?.contentHashCode() ?: 0)
        return result
    }
}

suspend fun RoomUser.toUserInfoElement(
    matrixClient: MatrixClient,
    initials: Initials,
    maxAvatarSize: Long
): UserInfoElement =
    UserInfoElement(
        name = this.name,
        userId = this.userId,
        initials = initials.compute(this.name),
        image = this.avatarUrl?.let {
            matrixClient.media.getMedia(it).getOrNull()?.limitedByteArrayOrNull(maxAvatarSize) {
                log.error { "Room image for room $roomId exceeds preview size limits, so it's not displayed" }
            }
        }
    )
