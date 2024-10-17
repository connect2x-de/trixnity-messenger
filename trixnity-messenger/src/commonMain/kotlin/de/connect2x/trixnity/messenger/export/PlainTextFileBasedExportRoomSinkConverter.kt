package de.connect2x.trixnity.messenger.export

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.core.model.RoomId

data class PlainTextFileBasedExportRoomProperties(
    override val destination: Destination
) : FileBasedExportRoomProperties

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
        val content = timelineEventContentToString(timelineEvent, filename)?.prependIndent(indent)
            ?: return null
        val sender = timelineEvent.sender.full
        val timestamp = exportTimestampMessageFormat.format(
            Instant.fromEpochMilliseconds(timelineEvent.originTimestamp).toLocalDateTime(timeZone)
        )
        return """
            $timestamp $sender:
            $content
        """.trimIndent() + "\r\n"
    }
}

private val exportTimestampMessageFormat by lazy {
    LocalDateTime.Format {
        year()
        chars("-")
        monthNumber()
        chars("-")
        dayOfMonth()
        chars(" ")
        hour()
        chars(":")
        minute()
        chars(":")
        second()
    }
}
