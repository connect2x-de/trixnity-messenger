package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import kotlinx.coroutines.flow.Flow
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.store.originalName
import net.folivo.trixnity.core.model.UserId

data class UserInfoElement(
    val name: String,
    val userId: UserId,
    val initials: String? = null,
    val image: Flow<ByteArray>? = null,
)

suspend fun RoomUser.toUserInfoElement(matrixClient: MatrixClient): UserInfoElement =
    UserInfoElement(
        name = this.originalName ?: this.name,
        userId = this.userId,
        initials = Initials.compute(this.originalName ?: this.name),
        image = this.avatarUrl?.let {
            matrixClient.media.getMedia(it).getOrNull()
        }
    )

