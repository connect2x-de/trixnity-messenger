package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.messenger.export.ExportRoomResult.Success.DecryptionFailed
import de.connect2x.trixnity.messenger.export.ExportRoomResult.Success.MissingMedia
import de.connect2x.trixnity.messenger.viewmodel.util.takeWhileInclusive
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.firstWithContent
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import okio.ByteString.Companion.toByteString
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

data class ExportRoomProgress(val processed: Long? = null, val total: Long? = null)

sealed interface ExportRoomResult {
    data object RoomNotFound : ExportRoomResult
    data class PropertiesNotSupported(val kClass: KClass<out ExportRoomSinkProperties>) : ExportRoomResult
    data class SinkError(val throwable: Throwable) : ExportRoomResult
    data class Success(
        val missingMedia: List<MissingMedia> = emptyList(),
        val decryptionFailed: List<DecryptionFailed> = emptyList(),
    ) : ExportRoomResult {
        data class MissingMedia(val eventId: EventId, val fileName: String?, val reason: String?)
        data class DecryptionFailed(val eventId: EventId, val reason: String?)
    }
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
        decryptionTimeout: Duration = 5.seconds,
        buffer: Int = 20,
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
        decryptionTimeout: Duration,
        buffer: Int,
    ): ExportRoomResult = coroutineScope {
        log.info { "export of $roomId started" }
        progress.value = ExportRoomProgress()

        val sink = sinkFactories.firstNotNullOfOrNull { it.create(roomId, properties) }
            ?: return@coroutineScope ExportRoomResult.PropertiesNotSupported(properties::class)
        val lastEventId = matrixClient.room.getById(roomId).firstOrNull()?.lastEventId
            ?: return@coroutineScope ExportRoomResult.RoomNotFound

        log.debug { "search for archiving start event of $roomId (may take some time due to network requests)" }
        val startFrom = matrixClient.room.getTimelineEvents(
            roomId = roomId,
            startFrom = lastEventId,
            direction = GetEvents.Direction.BACKWARDS,
            config = { this.decryptionTimeout = Duration.ZERO } // don't decrypt yet
        )
            .takeWhile { !rangeStartCondition(it.first()) }
            .onEach { progress.update { it.copy(total = (it.total ?: 0) + 1) } }
            .last().first().eventId
        log.debug { "start archiving of $roomId (start=$startFrom, total=${progress.value.total})" }

        sink.start().onFailure { return@coroutineScope ExportRoomResult.SinkError(it) }

        val missingMedia = mutableListOf<MissingMedia>()
        val decryptionFailed = mutableListOf<DecryptionFailed>()
        matrixClient.room.getTimelineEvents(
            roomId = roomId,
            startFrom = startFrom,
            direction = GetEvents.Direction.FORWARDS,
            config = { this.decryptionTimeout = decryptionTimeout }
        )
            .buffer(buffer)
            .takeWhile { !rangeEndCondition(it.first()) }
            .takeWhileInclusive { it.first().eventId != lastEventId }
            .map { flow -> async { flow.firstWithContent() } }
            .chunked(buffer)
            .map { list ->
                log.trace { "wait for chunk to be processed (size=${list.size})" }
                list.awaitAll()
                    .also { log.trace { "chunk fully processed (size=${list.size})" } }
            }
            .transform { list -> list.forEach { emit(it) } }
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
                                    content.info?.mimeType?.let {
                                        try {
                                            ContentType.parse(it)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }?.fileExtensions()?.firstOrNull()
                                if (extension != null) "$baseName.$extension"
                                else baseName
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
                        missingMedia.add(MissingMedia(timelineEvent.eventId, fileName, it.message))
                    }?.getOrNull()
                    if (fileName != null && media != null) {
                        sink.processTimelineEvent(timelineEvent, ExportRoomSink.Media(media, fileName))
                    } else {
                        sink.processTimelineEvent(timelineEvent)
                    }
                } else {
                    val exception = timelineEvent.content?.exceptionOrNull()
                    if (exception is TimelineEvent.TimelineEventContentError) {
                        when (exception) {
                            TimelineEvent.TimelineEventContentError.DecryptionAlgorithmNotSupported,
                            is TimelineEvent.TimelineEventContentError.DecryptionError,
                            TimelineEvent.TimelineEventContentError.DecryptionTimeout,
                            -> decryptionFailed.add(DecryptionFailed(timelineEvent.eventId, exception.message))

                            TimelineEvent.TimelineEventContentError.NoContent -> {}
                        }
                    }
                    sink.processTimelineEvent(timelineEvent)
                }
                progress.update { it.copy(processed = (it.processed ?: 0) + 1) }
                log.trace { "archiving progress: (${progress.value.processed}/${progress.value.total})" }
            }

        sink.finish().onFailure { return@coroutineScope ExportRoomResult.SinkError(it) }

        log.info { "export of $roomId finished" }
        ExportRoomResult.Success(missingMedia.toList(), decryptionFailed.toList())
    }
}

// TODO remove with kotlinx-coroutines >= 1.9.0
private fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> {
    require(size >= 1) { "Expected positive chunk size, but got $size" }
    return flow {
        var result: ArrayList<T>? = null // Do not preallocate anything
        collect { value ->
            // Allocate if needed
            val acc = result ?: ArrayList<T>(size).also { result = it }
            acc.add(value)
            if (acc.size == size) {
                emit(acc)
                // Cleanup, but don't allocate -- it might've been the case this is the last element
                result = null
            }
        }
        result?.let { emit(it) }
    }
}
