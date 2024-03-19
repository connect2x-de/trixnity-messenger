package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.messenger.export.ExportRoomResult.SuccessWithMissingMedia.MissingMedia
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import okio.ByteString.Companion.toByteString
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

data class ExportRoomProgress(val processed: Long? = null, val total: Long? = null)

sealed interface ExportRoomResult {
    data object RoomNotFound : ExportRoomResult
    data class PropertiesNotSupported(val kClass: KClass<out ExportRoomSinkProperties>) : ExportRoomResult
    data class SinkError(val throwable: Throwable) : ExportRoomResult
    data class SuccessWithMissingMedia(val missingMedia: List<MissingMedia>) : ExportRoomResult {
        data class MissingMedia(val name: String, val sender: UserId, val timestamp: Instant, val reason: String?)
    }

    data object Success : ExportRoomResult
}

interface ExportRoom {
    /**
     * To find the export start position, archiving is started from the last known event. When the start position is found,
     * the actual archiving is started. This means, that at first the [rangeStartCondition] is checked and after that [rangeEndCondition].
     */
    suspend operator fun invoke(
        roomId: RoomId,
        properties: ExportRoomSinkProperties,
        matrixClient: MatrixClient,
        rangeStartCondition: ExportRoomRangeStartCondition = ExportRoomRangeStartCondition.firstEvent(),
        rangeEndCondition: ExportRoomRangeEndCondition = ExportRoomRangeEndCondition.lastEvent(),
        progress: MutableStateFlow<ExportRoomProgress> = MutableStateFlow(ExportRoomProgress()),
    ): ExportRoomResult
}

class ExportRoomImpl(
    private val sinkFactories: List<ExportRoomSinkFactory>,
) : ExportRoom {
    override suspend fun invoke(
        roomId: RoomId,
        properties: ExportRoomSinkProperties,
        matrixClient: MatrixClient,
        rangeStartCondition: ExportRoomRangeStartCondition,
        rangeEndCondition: ExportRoomRangeEndCondition,
        progress: MutableStateFlow<ExportRoomProgress>,
    ): ExportRoomResult {
        progress.value = ExportRoomProgress()
        val withDecryptionTimeout = 5.seconds
        val buffer = 10

        val sink = sinkFactories.firstNotNullOfOrNull { it.create(roomId, properties) }
            ?: return ExportRoomResult.PropertiesNotSupported(properties::class)
        val lastEventId = matrixClient.room.getById(roomId).firstOrNull()?.lastEventId
            ?: return ExportRoomResult.RoomNotFound

        log.debug { "search for start event (may take some time due to network request)" }
        val startFrom = matrixClient.room.getTimelineEvents(
            roomId = roomId,
            startFrom = lastEventId,
            direction = GetEvents.Direction.BACKWARDS,
            config = { decryptionTimeout = withDecryptionTimeout }
        )
            .map { flow -> flow.first { it.content != null } }
            .takeWhile { !rangeStartCondition(it) }
            .onEach { progress.update { it.copy(total = (it.total ?: 0) + 1) } }
            .last().eventId
        log.debug { "start archiving from $startFrom" }

        sink.start().onFailure { return ExportRoomResult.SinkError(it) }

        val missingMedia = mutableListOf<MissingMedia>()
        matrixClient.room.getTimelineEvents(
            roomId = roomId,
            startFrom = startFrom,
            direction = GetEvents.Direction.FORWARDS,
            config = { decryptionTimeout = withDecryptionTimeout }
        )
            .map { flow -> flow.first { it.content != null } }
            .takeWhile { !rangeEndCondition(it) }
            .buffer(buffer)
            .collect { timelineEvent ->
                val content = timelineEvent.content?.getOrNull()
                if (content is RoomMessageEventContent.FileBased) {
                    val mediaUrl = content.url
                    val mediaFile = content.file
                    val fileName =
                        (mediaFile?.url ?: mediaUrl)
                            ?.encodeToByteArray()?.toByteString()?.base64Url()?.substringBefore("=")
                            ?.let { baseName ->
                                val extension =
                                    content.info?.mimeType?.let(ContentType::parse)?.fileExtensions()?.firstOrNull()
                                if (extension != null) "$baseName.$extension"
                                else baseName // TODO as fallback: look into first bytes for file extension (https://en.wikipedia.org/wiki/List_of_file_signatures)
                            }
                            ?: run {
                                log.warn { "content did not contain any url" }
                                null
                            }
                    val media = when {
                        mediaUrl != null -> matrixClient.media.getMedia(mediaUrl, saveToCache = false)
                        mediaFile != null -> matrixClient.media.getEncryptedMedia(mediaFile, saveToCache = false)
                        else -> null
                    }?.onFailure {
                        missingMedia.add(
                            MissingMedia(
                                content.body,
                                timelineEvent.sender,
                                Instant.fromEpochMilliseconds(timelineEvent.originTimestamp),
                                it.message
                            )
                        )
                    }?.getOrNull()
                    if (fileName != null && media != null) {
                        sink.processTimelineEvent(timelineEvent, ExportRoomSink.Media(media, fileName))
                    } else {
                        sink.processTimelineEvent(timelineEvent)
                    }
                } else {
                    sink.processTimelineEvent(timelineEvent)
                }
                progress.update { it.copy(processed = (it.processed ?: 0) + 1) }
            }

        sink.finish().onFailure { return ExportRoomResult.SinkError(it) }

        return if (missingMedia.isNotEmpty()) ExportRoomResult.SuccessWithMissingMedia(missingMedia)
        else ExportRoomResult.Success
    }
}
