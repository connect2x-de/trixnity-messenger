package de.connect2x.trixnity.messenger.viewmodel.room.archive

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.fileBaseArchiveSink
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import okio.ByteString.Companion.toByteString
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

class PlainTexArchiveSink(private val i18n: I18n) : ArchiveSink {

    private val formatExtension: String = ".txt"
    override val sinkName: String
        get() = i18n.textPlainFormat()
    override val archiveSinkState: MutableStateFlow<ArchiveSinkState>
        get() = MutableStateFlow(ArchiveSinkState.None)

    private fun createFileName(sinkConfig: PlainTextArchiveSinkConfig, roomId: RoomId): String {
        return if (sinkConfig.fileName == null) {
            // Create default name
            val roomIdAsUnPaddedBase64 = roomId.full.encodeToByteArray().toByteString().base64Url().substringBefore("=")
            val currentTimeStamp = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
                .toLocalDateTime(TimeZone.of(timezone())).formatLocalDateTime()
            "${currentTimeStamp}_${roomIdAsUnPaddedBase64}${formatExtension}"
        } else {
            // Create user provided fileName
            "${sinkConfig.fileName}${formatExtension}"
        }
    }

    fun archivePlainText(
        matrixClient: MatrixClient, roomId: RoomId,
        sinkConfig: ArchiveSinkConfig,
        coroutineScope: CoroutineScope
    ) {
        if (archiveSinkState.value == ArchiveSinkState.Loading)
            return

        if (sinkConfig !is PlainTextArchiveSinkConfig) {
            log.error { "Unsupported configuration provided.. " }
            return
        }

        archiveSinkState.value = ArchiveSinkState.Loading
        coroutineScope.launch {
            val lastEventId = matrixClient.room.getById(roomId).firstOrNull()?.lastEventId
            lastEventId?.let { eventId ->
                matrixClient.room.getTimelineEvents(
                    roomId,
                    startFrom = eventId,
                    config = { decryptionTimeout = 5.seconds })
                    .onStart { createFileName(sinkConfig, roomId) }
                    .onCompletion { cause ->
                        if (cause != null) {
                            archiveSinkState.value = ArchiveSinkState.Error(i18n.archiveRoomError())
                            log.error(cause) { "export failed.." }
                        } else {
                            archiveSinkState.value = ArchiveSinkState.Success
                        }
                    }.collect { timeLineFlow ->
                        val timelineEvent = timeLineFlow.first { it.content != null }
                        val content = transformMessage(timelineEvent)
                        if (content != null) {
                            fileBaseArchiveSink(
                                createFileName(sinkConfig, roomId), content
                            )
                        }
                    }

            } ?: kotlin.run {
                log.warn { "Room does not contain any data." }
                archiveSinkState.value = ArchiveSinkState.Error(i18n.archiveRoomError())
            }

        }
    }

    private fun transformMessage(timelineEvent: TimelineEvent): String? {
        val sender = timelineEvent.sender.full
        val event = timelineEvent.event
        val receivedDateTime = localDateTimeOf(event).formatLocalDateTime()
        val formattedResult = timelineEvent.content?.fold(onSuccess = {
            if (it is RoomMessageEventContent.TextBased) {
                "$receivedDateTime $sender: ${it.body}"
            } else
                null
        }, onFailure = {
            log.error(it) { "failed to archive room" }
            null
        })

        return formattedResult
    }

    private fun LocalDateTime.formatLocalDateTime(): String =
        "${formatDate(this)},${formatTime(this)}"

    private fun localDateTimeOf(event: ClientEvent.RoomEvent<*>): LocalDateTime {
        val timestamp = event.originTimestamp
        requireNotNull(timestamp) // should not happen as only RoomEvents and StateEvents are possible
        return Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.of(timezone()))
    }
}
