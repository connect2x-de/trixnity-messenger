package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.store.originTimestamp
import de.connect2x.trixnity.client.store.sender
import de.connect2x.trixnity.core.model.RoomId
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class PlainTextFileBasedExportRoomProperties(override val destination: Destination) : FileBasedExportRoomProperties

class PlainTextFileBasedExportRoomSinkConverterFactory(
    private val timelineEventContentToString: TimelineEventContentToString,
    private val timeZone: TimeZone,
) : FileBasedExportRoomSinkConverterFactory {
    override fun create(roomId: RoomId, properties: FileBasedExportRoomProperties): FileBasedExportRoomSinkConverter? =
        if (properties is PlainTextFileBasedExportRoomProperties)
            PlainTextFileBasedExportRoomSinkConverter(timelineEventContentToString, timeZone)
        else null
}

class PlainTextFileBasedExportRoomSinkConverter(
    private val timelineEventContentToString: TimelineEventContentToString,
    private val timeZone: TimeZone,
) : FileBasedExportRoomSinkConverter {
    override val extension: String = "txt"
    private val indent = "    "

    override suspend fun convert(timelineEvent: TimelineEvent, filename: String?): String? {
        val content = timelineEventContentToString(timelineEvent, filename)?.prependIndent(indent) ?: return null
        val sender = timelineEvent.sender.full
        val timestamp =
            exportTimestampMessageFormat.format(
                Instant.fromEpochMilliseconds(timelineEvent.originTimestamp).toLocalDateTime(timeZone)
            )
        return """
            $timestamp $sender:
            $content
        """
            .trimIndent() + "\r\n"
    }
}

private val exportTimestampMessageFormat by lazy {
    LocalDateTime.Format {
        year()
        chars("-")
        monthNumber()
        chars("-")
        day()
        chars(" ")
        hour()
        chars(":")
        minute()
        chars(":")
        second()
    }
}
