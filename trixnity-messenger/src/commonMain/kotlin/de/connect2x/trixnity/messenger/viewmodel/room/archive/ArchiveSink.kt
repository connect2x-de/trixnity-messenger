package de.connect2x.trixnity.messenger.viewmodel.room.archive

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.fileBaseArchiveSink
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.io.async.launch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
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

interface ArchiveSink {
    val sinkName: String
}

sealed interface ArchiveSinkState {
    data object None : ArchiveSinkState
    data object Loading : ArchiveSinkState
    data object Success : ArchiveSinkState
    data class Error(val error: String) : ArchiveSinkState
}


class PlainTextFormat(
    private val roomId: RoomId,
    private val matrixClient: MatrixClient,
    private val viewModelContext: ViewModelContext,
    private val sinkConfig: PlainTextArchiveSinkConfig
) : ArchiveSink {

    val i18n = viewModelContext.i18n
    internal val archiveSinkState: MutableStateFlow<ArchiveSinkState> = MutableStateFlow(ArchiveSinkState.None)

    private fun createFileName(): String {
        return if (sinkConfig.fileName == null) {
            val roomIdAsUnPaddedBase64 = roomId.full.encodeToByteArray().toByteString().base64Url().substringBefore("=")
            val currentTimeStamp = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
                .toLocalDateTime(TimeZone.of(timezone())).formatLocalDateTime()
             "${currentTimeStamp}_${roomIdAsUnPaddedBase64}${".txt"}"
        }else{
            "${sinkConfig.fileName}${".txt"}"
        }
    }

    fun archivePlainText() {
        if (archiveSinkState.value == ArchiveSinkState.Loading) return

        archiveSinkState.value = ArchiveSinkState.Loading
        viewModelContext.coroutineScope.launch {
            val batchedArchiveResultContent = mutableListOf<String>()
            val lastEventId = matrixClient.room.getById(roomId).firstOrNull()?.lastEventId
            lastEventId?.let { eventId ->
                matrixClient.room.getTimelineEvents(
                    roomId,
                    startFrom = eventId,
                    config = { decryptionTimeout = 5.seconds })
                    .onStart {createFileName()}
                    .onCompletion { cause ->
                        if (cause != null) {
                            archiveSinkState.value = ArchiveSinkState.Error(i18n.archiveRoomError())
                            log.error(cause) { "export failed.." }
                        } else {
                            if (batchedArchiveResultContent.isNotEmpty()) {
                                fileBaseArchiveSink(createFileName(), batchedArchiveResultContent.joinToString("\n"))
                                batchedArchiveResultContent.clear()
                            }
                            archiveSinkState.value = ArchiveSinkState.Success
                        }
                    }
                    .buffer(capacity = 30, onBufferOverflow = BufferOverflow.DROP_OLDEST)
                    .transform<Flow<TimelineEvent>, List<String>> { timeLineFlow ->
                        val timelineEvent = timeLineFlow.first { it.content != null }
                        val content = transformMessage(timelineEvent)
                        if (content != null) {
                            batchedArchiveResultContent.add(content)
                        }
                        // Emit the batch if there are any buffered events
                        if (batchedArchiveResultContent.isNotEmpty() && batchedArchiveResultContent.size == 30) {
                            emit(batchedArchiveResultContent)
                            batchedArchiveResultContent.clear()
                        }

                    }.collect { batchContent ->
                        fileBaseArchiveSink(createFileName(), batchedArchiveResultContent.joinToString("\n"))
                    }

            } ?: kotlin.run {
                log.warn { "Room does not contain any data." }
                archiveSinkState.value = ArchiveSinkState.Error(i18n.archiveRoomError())
            }

        }
    }

//    viewModelContext. .launch {
//        val lastEventId = matrixClient.room.getById(selectedRoomId).firstOrNull()?.lastEventId
//        lastEventId?.let {
//            matrixClient.room.getTimelineEvents(
//                selectedRoomId,
//                startFrom = lastEventId,
//                config = { decryptionTimeout = 5.seconds })
//                .onStart {
//                    archiveResultProcessor.setupFileNameParameters(
//                        selectedRoomId,
//                        selectedSinkFormat.value.formatExtension
//                    )
//                    val selectedSinkFormat = selectedSinkFormat.value
//                    if (selectedSinkFormat is  CSVArchiveFormat){
//                        selectedSinkFormat.updateColumnNames()
//                    }
//                }
//                .onCompletion { cause ->
//                    if (cause != null) {
//                        archiveRoomState.value = ArchiveRoomState.None
//                        archiveRoomState.value = ArchiveRoomState.Error(i18n.archiveRoomError())
//                        de.connect2x.trixnity.messenger.viewmodel.room.settings.log.error(cause) { "export failed.." }
//                    } else {
//                        if (batchedArchiveResultContent.isNotEmpty()) {
//                            archiveResultProcessor.processResult(batchedArchiveResultContent.joinToString("\n"))
//                            batchedArchiveResultContent.clear()
//                        }
//                        archiveRoomState.value = ArchiveRoomState.Success
//                    }
//                }
//                .buffer(capacity = 30, onBufferOverflow = BufferOverflow.DROP_OLDEST)
//                .transform<Flow<TimelineEvent>, List<String>> { timeLineFlow ->
//                    val timelineEvent = timeLineFlow.first { it.content != null }
//                    val content = selectedSinkFormat.value.transformMessage(timelineEvent)
//                    if (content != null) {
//                        batchedArchiveResultContent.add(content)
//                    }
//                    // Emit the batch if there are any buffered events
//                    if (batchedArchiveResultContent.isNotEmpty() && batchedArchiveResultContent.size == 30) {
//                        emit(batchedArchiveResultContent)
//                        batchedArchiveResultContent.clear()
//                    }
//
//                }.collect { batchContent ->
//                    archiveResultProcessor.processResult(batchContent.joinToString("\n"))
//                }
//        } ?: run {
//            de.connect2x.trixnity.messenger.viewmodel.room.settings.log.warn { "Room does not contain any data." }
//            archiveRoomState.value = ArchiveRoomState.Error(i18n.archiveRoomError())
//        }
//    }
//


    //    override val formatExtension: String
//        get() = ".txt"
//    override val formatName: String
//        get() = i18n.textPlainFormat()
//
    suspend fun transformMessage(timelineEvent: TimelineEvent): String? {
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

    override val sinkName: String
        get() = i18n.textPlainFormat()


}

class CSVArchiveFormat(private val i18n: I18n) : ArchiveSink {

    //    override val formatExtension: String
//        get() = ".csv"
//    override val formatName: String
//        get() = i18n.csvFormat()
//
//    private var appendColumnNames: Boolean = false
//
//    fun updateColumnNames() {
//        appendColumnNames = true
//    }
//
//    override suspend fun transformMessage(timelineEvent: TimelineEvent): String? {
//        val sender = timelineEvent.sender.full
//        val event = timelineEvent.event
//        val receivedDateTime = localDateTimeOf(event).formatLocalDateTime()
//        val formattedResult = timelineEvent.content?.fold(onSuccess = {
//            if (it is RoomMessageEventContent.TextBased) {
//                if (appendColumnNames) {
//                    val columnHeadings =
//                        """${i18n.csvFormatDateHeading()}, ${i18n.csvFormatTimeHeading()}, ${i18n.csvFormatSenderHeading()},  ${i18n.csvFormatMessageHeading()}"""
//                    val headingContent = columnHeadings + "\n" + "$receivedDateTime, $sender, ${it.body}"
//                    appendColumnNames = false
//                    headingContent
//                } else {
//                    "$receivedDateTime, $sender, ${it.body}"
//                }
//            } else
//                null
//        }, onFailure = {
//            log.error(it) { "failed to archive room" }
//            null
//        })
//
//        return formattedResult
//    }
    override val sinkName: String
        get() = i18n.csvFormat()

}


internal fun LocalDateTime.formatLocalDateTime(): String =
    "${formatDate(this)},${formatTime(this)}"

private fun localDateTimeOf(event: ClientEvent.RoomEvent<*>): LocalDateTime {
    val timestamp = event.originTimestamp
    requireNotNull(timestamp) // should not happen as only RoomEvents and StateEvents are possible
    return Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.of(timezone()))
}
