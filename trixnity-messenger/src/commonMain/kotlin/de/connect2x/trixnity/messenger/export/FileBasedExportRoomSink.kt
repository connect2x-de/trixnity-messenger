package de.connect2x.trixnity.messenger.export

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.core.model.RoomId
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.ByteString.Companion.toByteString

expect class Destination

interface FileBasedExportRoomProperties : ExportRoomSinkProperties {
    val destination: Destination
}

class FileBasedExportRoomSinkFactory(
    private val converterFactories: List<FileBasedExportRoomSinkConverterFactory>,
    private val writerFactory: FileBasedExportRoomSinkWriterFactory,
    private val clock: Clock,
    private val timeZone: TimeZone,
) : ExportRoomSinkFactory {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.export.FileBasedExportRoomSink")
    }

    override fun create(roomId: RoomId, properties: ExportRoomSinkProperties): ExportRoomSink? {
        if (properties !is FileBasedExportRoomProperties) return null
        val converter =
            converterFactories.firstNotNullOfOrNull { it.create(roomId, properties) }
                ?: run {
                    log.warn { "properties are not supported" }
                    return null
                }
        val roomIdAsUnPaddedBase64 = roomId.full.encodeToByteArray().toByteString().base64Url()
        val currentTimestamp = exportTimestampFormat.format(clock.now().toLocalDateTime(timeZone))
        val fileName = "$currentTimestamp ${roomIdAsUnPaddedBase64}.${converter.extension}"

        val writer = writerFactory.create(properties.destination, fileName)
        return FileBasedExportRoomSinkBase(converter = converter, writer = writer)
    }
}

class FileBasedExportRoomSinkBase(
    private val converter: FileBasedExportRoomSinkConverter,
    private val writer: FileBasedExportRoomSinkWriter,
) : ExportRoomSink {
    override suspend fun start(): Result<Unit> = kotlin.runCatching {
        writer.start()
        converter.prefix()?.let { writer.addContent(it) }
    }

    override suspend fun finish(): Result<Unit> = kotlin.runCatching {
        converter.suffix()?.let { writer.addContent(it) }
        writer.finish()
    }

    override suspend fun processTimelineEvent(
        timelineEvent: TimelineEvent,
        media: ExportRoomSink.Media?,
    ): Result<Unit> = kotlin.runCatching {
        val nextContent = converter.convert(timelineEvent, media?.fileName)
        if (nextContent != null) {
            writer.addContent(nextContent)
            if (media != null) writer.addMedia(media.content, media.fileName)
        }
    }
}
