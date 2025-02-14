package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.core.model.UserId


private val log = KotlinLogging.logger {}

class UserInfoElement(
    val userId: UserId,
    val name: String,
    val initials: String,
    val image: StateFlow<ByteArray?>? = null,
    val imageUrl: String? = null,
)

fun RoomUser?.toUserInfoElement(
    coroutineScope: CoroutineScope,
    matrixClient: MatrixClient,
    initials: Initials,
    maxAvatarSize: Long,
    fallbackUserId: UserId,
): UserInfoElement =
    UserInfoElement(
        name = this?.name ?: fallbackUserId.full,
        userId = this?.userId ?: fallbackUserId,
        initials = initials.compute(this?.name ?: fallbackUserId.full),
        image = this@toUserInfoElement?.avatarUrl?.let { avatarUrl ->
            flow {

                // TODO: some sort of retry (see retryLoopFlow)
                emit(
                    matrixClient.media.getMedia(avatarUrl).getOrNull()?.limitedByteArrayOrNull(maxAvatarSize) {
                        log.error { "Room image for room $roomId exceeds preview size limits, so it's not displayed" }
                    }
                )
            }.stateIn(coroutineScope, WhileSubscribed(), null)
        },
        imageUrl = this@toUserInfoElement?.avatarUrl,
    )

fun RoomUser.toUserInfoElement(
    coroutineScope: CoroutineScope,
    matrixClient: MatrixClient,
    initials: Initials,
    maxAvatarSize: Long,
): UserInfoElement = toUserInfoElement(coroutineScope, matrixClient, initials, maxAvatarSize, userId)
