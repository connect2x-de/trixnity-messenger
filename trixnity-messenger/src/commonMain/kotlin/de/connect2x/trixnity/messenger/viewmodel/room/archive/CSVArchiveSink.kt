package de.connect2x.trixnity.messenger.viewmodel.room.archive

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.fileBaseArchiveSink
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.core.model.RoomId
import okio.ByteString.Companion.toByteString
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

class CSVArchiveSink(
    private val i18n: I18n,
    val matrixClient: MatrixClient,
    val roomId: RoomId,
    val sinkConfig: ArchiveSinkConfig,
) : ArchiveSink {

    private val formatExtension: String = ".csv"
    override suspend fun processArchive(archiveStateCallback: (ArchiveSinkState) -> Unit) {
        if (sinkConfig !is GetCSVArchiveSinkConfig) {
            log.error { "Unsupported configuration provided.." }
            archiveStateCallback(ArchiveSinkState.None)
            return
        }

        val lastEventId = matrixClient.room.getById(roomId).firstOrNull()?.lastEventId
        lastEventId?.let { eventId ->
            matrixClient.room.getTimelineEvents(
                roomId,
                startFrom = eventId,
                config = { decryptionTimeout = 5.seconds })
                .onStart {
                    archiveStateCallback(ArchiveSinkState.Loading)
                    createFileName(sinkConfig, roomId)
                    // If user want to have column heading so this way you can add column title for CSV
                    if (sinkConfig.requireHeadingLabels) {
                        val columnHeadings =
                            """${i18n.csvFormatDateHeading()}, ${i18n.csvFormatTimeHeading()}, ${i18n.csvFormatSenderHeading()},  ${i18n.csvFormatMessageHeading()}"""
                        fileBaseArchiveSink(createFileName(sinkConfig, roomId), columnHeadings)
                    }
                }
                .onCompletion { cause ->
                    if (cause != null) {
                        archiveStateCallback(ArchiveSinkState.Error(i18n.archiveRoomError()))
                        log.error(cause) { "export failed.." }
                    } else {
                        log.error { "Sink successfully complete for $roomId" }
                        archiveStateCallback(ArchiveSinkState.Success)

                    }
                }.collect { timeLineFlow ->
                    val timelineEvent = timeLineFlow.first { it.content != null }
                    val content = transformMessage(timelineEvent)
                    if (content != null) {
                        fileBaseArchiveSink(createFileName(sinkConfig, roomId), content)
                    }
                }

        } ?: kotlin.run {
            log.warn { "Room does not contain any data." }
            archiveStateCallback(ArchiveSinkState.Error(i18n.archiveRoomError()))
        }
    }

    override val archiveSinkState: MutableStateFlow<ArchiveSinkState> get() = MutableStateFlow(ArchiveSinkState.None)


    private fun createFileName(plainTextArchiveSinkConfig: GetCSVArchiveSinkConfig, roomId: RoomId): String {
        return if (plainTextArchiveSinkConfig.fileName == null) {
            // Create default name
            val roomIdAsUnPaddedBase64 = roomId.full.encodeToByteArray().toByteString().base64Url().substringBefore("=")
            val currentTimeStamp = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
                .toLocalDateTime(TimeZone.of(timezone())).formatLocalDateTime()
            "${currentTimeStamp}_${roomIdAsUnPaddedBase64}${formatExtension}"
        } else {
            // Create user provided fileName
            "${plainTextArchiveSinkConfig.fileName}${formatExtension}"
        }
    }
}
