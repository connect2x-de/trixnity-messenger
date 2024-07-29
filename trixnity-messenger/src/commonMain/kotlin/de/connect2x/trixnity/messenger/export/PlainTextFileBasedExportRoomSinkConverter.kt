package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.messenger.viewmodel.util.timezone
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
    private val timelineEventContentToString: TimelineEventContentToString
) : FileBasedExportRoomSinkConverterFactory {
    override fun create(roomId: RoomId, properties: FileBasedExportRoomProperties): FileBasedExportRoomSinkConverter? =
        if (properties is PlainTextFileBasedExportRoomProperties)
            PlainTextFileBasedExportRoomSinkConverter(timelineEventContentToString)
        else null
}

class PlainTextFileBasedExportRoomSinkConverter(
    private val timelineEventContentToString: TimelineEventContentToString
) : FileBasedExportRoomSinkConverter {
    override val extension: String = "txt"
    private val indent = "    "

    override suspend fun convert(timelineEvent: TimelineEvent, filename: String?): String? {
        val content = timelineEventContentToString(timelineEvent, filename)?.prependIndent(indent)
            ?: return null
        val sender = timelineEvent.sender.full
        val timestamp = exportTimestampMessageFormat.format(
            Instant.fromEpochMilliseconds(timelineEvent.originTimestamp).toLocalDateTime(TimeZone.of(timezone()))
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
