package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.RoomId
import okio.ByteString.Companion.toByteString

private val log = KotlinLogging.logger { }

interface FileBasedExportRoomProperties : ExportRoomSinkProperties {
    val destination: FileDescriptor
}

class FileBasedExportRoomSinkFactory(
    private val converterFactories: List<FileBasedExportRoomSinkConverterFactory>,
    private val writerFactory: FileBasedExportRoomSinkWriterFactory,
    private val clock: Clock,
) : ExportRoomSinkFactory {
    override fun create(roomId: RoomId, properties: ExportRoomSinkProperties): ExportRoomSink? {
        if (properties !is FileBasedExportRoomProperties) return null
        val converter = converterFactories.firstNotNullOfOrNull { it.create(roomId, properties) }
            ?: run {
                log.warn { "properties are not supported" }
                return null
            }
        val roomIdAsUnPaddedBase64 = roomId.full.encodeToByteArray().toByteString().base64Url().substringBefore("=")
        val currentTimeStamp =
            LocalDateTime.Formats.ISO.format(
                Instant.fromEpochMilliseconds(clock.now().toEpochMilliseconds())
                    .toLocalDateTime(TimeZone.of(timezone()))
            )
        val fileName = "${currentTimeStamp}_${roomIdAsUnPaddedBase64}.${converter.extension}"

        val writer = writerFactory.create(roomId, properties.destination, fileName)
        return FileBasedExportRoomSinkBase(
            converter = converter,
            writer = writer,
        )
    }
}

class FileBasedExportRoomSinkBase(
    private val converter: FileBasedExportRoomSinkConverter,
    private val writer: FileBasedExportRoomSinkWriter,
) : ExportRoomSink {
    override suspend fun start(): Result<Unit> = kotlin.runCatching {
        writer.createFilesAndDirectories()
        converter.prefix()?.let { writer.addContent(it) }
    }

    override suspend fun finish(): Result<Unit> = kotlin.runCatching {
        converter.suffix()?.let { writer.addContent(it) }
    }

    override suspend fun processTimelineEvent(
        timelineEvent: TimelineEvent,
        media: ExportRoomSink.Media?
    ): Result<Unit> = kotlin.runCatching {
        val nextContent = converter.convert(timelineEvent, media?.fileName)
        if (nextContent != null) {
            writer.addContent(nextContent)
            if (media != null) writer.addMedia(media.content, media.fileName)
        }
    }
}
