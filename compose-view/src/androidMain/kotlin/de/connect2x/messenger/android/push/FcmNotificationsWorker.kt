package de.connect2x.messenger.android.push

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.connect2x.messenger.android.backgroundSyncShouldBeRunning
import de.connect2x.messenger.android.withMatrixMessengerService
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.PushMode
import de.connect2x.trixnity.messenger.platformNotifications
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

class FcmNotificationsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    init {
        log.debug { "init FcmPushWorker" }
    }

    override suspend fun doWork(): Result =
        withMatrixMessengerService(applicationContext) { matrixMultiMessenger ->
            val matrixMessenger = matrixMultiMessenger.activeMatrixMessenger.value
                ?: return@withMatrixMessengerService Result.failure()
            coroutineScope {
                // we cannot assume that we should still be running
                val pushModes =
                    matrixMessenger.di.get<MatrixMessengerSettingsHolder>().value.base.accounts.map { it.value.platformNotifications.pushMode }
                if (pushModes.none { it == PushMode.PUSH } ||
                    applicationContext.backgroundSyncShouldBeRunning.not()
                ) {
                    return@coroutineScope Result.failure()
                }

                val roomId = inputData.getString("roomId")?.let(::RoomId)
                val eventId = inputData.getString("eventId")?.let(::EventId)
                if (roomId == null || eventId == null) return@coroutineScope Result.failure()

                log.debug { "got event $eventId in room $roomId" }

                findTheCorrespondingMatrixClient(
                    matrixMessenger,
                    roomId,
                )?.let { matrixClient ->
                    val listen = async {
                        listenToNotifications(
                            applicationContext,
                            matrixMessenger,
                            matrixClient,
                            roomId,
                            eventId,
                        )
                    }

                    val result = matrixClient.syncOnce {}
                    result.onFailure {
                        log.error(it) { "cannot get the event $eventId in room $roomId" }
                        listen.cancel()
                        return@coroutineScope Result.retry()
                    }
                    val listenResult = withTimeoutOrNull(5.seconds) {
                        listen.await()
                    }
                    if (listenResult == null) {
                        log.warn { "Received no notification for event $eventId in room $roomId" }
                    }

                    result.onSuccess {
                        log.debug { "receive push event $eventId in room $roomId was successful -> end worker" }
                        return@coroutineScope Result.success()
                    }
                }
                return@coroutineScope Result.failure()
            }
        }

    // since the account name is not known beforehand, we have to retrieve it here by checking which MatrixClient the
    // roomId belongs to
    // TODO this does not find rooms from invites (requires a sync)
    private suspend fun findTheCorrespondingMatrixClient(
        matrixMessenger: MatrixMessenger,
        roomId: RoomId,
    ): MatrixClient? {
        val matrixClient =
            matrixMessenger.di.get<MatrixClients>().value.values.firstOrNull { matrixClient ->
                matrixClient.room.getById(roomId).first() != null
            }

        if (matrixClient == null) {
            log.warn { "cannot find a MatrixClient for the room $roomId" }
        }
        return matrixClient
    }
}
