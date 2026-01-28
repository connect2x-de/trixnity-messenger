package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.store.avatarUrl
import de.connect2x.trixnity.core.model.UserId

data class UserInfoElement(
    val userId: UserId,
    val name: String,
    val initials: String,
    val image: StateFlow<ByteArray?>? = null,
) {
    override fun toString(): String {
        return "UserInfoElement(userId=$userId, name='$name', initials='$initials', image(size)=${image?.value?.size})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UserInfoElement

        if (userId != other.userId) return false
        if (name != other.name) return false
        if (initials != other.initials) return false
        // without image to enable real equality check as even if the same user is represented by this class, the image would be new

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + initials.hashCode()
        // without image to enable real equality check as even if the same user is represented by this class, the image would be new
        return result
    }
}

fun RoomUser?.toUserInfoElement(
    coroutineScope: CoroutineScope,
    matrixClient: MatrixClient,
    initials: Initials,
    fallbackUserId: UserId,
    maxMediaSizeInMemory: Long,
): UserInfoElement =
    UserInfoElement(
        name = this?.name ?: fallbackUserId.full,
        userId = this?.userId ?: fallbackUserId,
        initials = initials.compute(this?.name ?: fallbackUserId.full),
        image = this@toUserInfoElement?.avatarUrl?.let { avatarUrl ->
            flow {
                // TODO some sort of retry (see retryLoopFlow)
                emit(
                    matrixClient.media.getThumbnail(avatarUrl, avatarSize().toLong(), avatarSize().toLong()).fold(
                        onSuccess = {
                            it.toByteArray(coroutineScope, maxSize = maxMediaSizeInMemory)
                        },
                        onFailure = { null }
                    )
                )
            }.stateIn(coroutineScope, WhileSubscribed(), null)
        }
    )

fun RoomUser.toUserInfoElement(
    coroutineScope: CoroutineScope,
    matrixClient: MatrixClient,
    initials: Initials,
    maxMediaSizeInMemory: Long,
): UserInfoElement = toUserInfoElement(coroutineScope, matrixClient, initials, userId, maxMediaSizeInMemory)
