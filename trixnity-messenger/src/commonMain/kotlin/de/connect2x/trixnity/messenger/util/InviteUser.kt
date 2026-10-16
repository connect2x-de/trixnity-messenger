package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.key
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import io.ktor.http.*
import kotlinx.coroutines.CancellationException

private val log = Logger("de.connect2x.trixnity.messenger.util.InviteUser")

fun interface InviteUser {
    suspend operator fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId,
        userId: UserId,
        reason: String?,
    ): Result<Unit>
}

class InviteUserImpl(private val config: MatrixMessengerConfiguration) : InviteUser {
    override suspend fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId,
        userId: UserId,
        reason: String?,
    ): Result<Unit> = runCatching {
        if (config.features.enableHistoricRoomKeySharing) {
            matrixClient.key
                .shareRoomKeyBundle(roomId, userId)
                .fold(
                    onSuccess = {},
                    onFailure = { exception ->
                        if (exception is CancellationException) throw exception
                        if (exception !is MatrixServerException) throw exception
                        if (
                            exception.statusCode == HttpStatusCode.Forbidden ||
                                exception.statusCode == HttpStatusCode.PayloadTooLarge
                        ) {
                            log.warn(exception) { "could not send historic room keys because it got rejected" }
                        }
                    },
                )
        }
        matrixClient.api.room.inviteUser(roomId, userId, reason).getOrThrow()
    }
}
