package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.store.eventId
import de.connect2x.trixnity.client.store.originTimestamp
import de.connect2x.trixnity.client.store.sender
import de.connect2x.trixnity.core.model.RoomId
import kotlin.time.Instant

data class CSVFileBasedExportRoomProperties(
    override val destination: Destination,
    val csvDateHeader: String = "date",
    val csvSenderHeader: String = "sender",
    val csvEventIdHeader: String = "eventId",
    val csvContentHeader: String = "content",
    val delimiter: String = ",",
) : FileBasedExportRoomProperties

class CSVFileBasedExportRoomSinkConverterFactory(
    private val timelineEventContentToString: TimelineEventContentToString,
) : FileBasedExportRoomSinkConverterFactory {
    override fun create(roomId: RoomId, properties: FileBasedExportRoomProperties): FileBasedExportRoomSinkConverter? =
        if (properties is CSVFileBasedExportRoomProperties)
            CSVFileBasedExportRoomSinkConverter(properties, timelineEventContentToString)
        else null
}

class CSVFileBasedExportRoomSinkConverter(
    private val properties: CSVFileBasedExportRoomProperties,
    private val timelineEventContentToString: TimelineEventContentToString,
) : FileBasedExportRoomSinkConverter {
    override val extension: String = "csv"

    private val prefix by lazy {
        listOf(
            properties.csvDateHeader,
            properties.csvSenderHeader,
            properties.csvEventIdHeader,
            properties.csvContentHeader
        ).asCsvLine()
    }

    override suspend fun prefix(): String = prefix

    override suspend fun convert(timelineEvent: TimelineEvent, filename: String?): String? {
        val content = timelineEventContentToString(timelineEvent, filename) ?: return null
        return listOf(
            Instant.fromEpochMilliseconds(timelineEvent.originTimestamp).toString(),
            timelineEvent.sender.full,
            timelineEvent.eventId.full,
            content,
        ).asCsvLine()
    }

    private fun List<String>.asCsvLine() =
        joinToString(separator = properties.delimiter) { "\"'" + it.replace("\"", "\"\"") + "\"" } +
                "\r\n"
}
