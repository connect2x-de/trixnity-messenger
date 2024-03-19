package de.connect2x.trixnity.messenger.export

import kotlinx.datetime.Instant
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.core.model.RoomId

data class CSVFileBasedExportRoomProperties(
    override val destination: Destination,
    val csvDateHeader: String = "date",
    val csvSenderHeader: String = "sender",
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
        listOf(properties.csvDateHeader, properties.csvSenderHeader, properties.csvContentHeader).asCsvLine()
    }

    override suspend fun prefix(): String = prefix

    override suspend fun convert(timelineEvent: TimelineEvent, filename: String?): String? {
        val content = timelineEventContentToString(timelineEvent, filename) ?: return null
        return listOf(
            timelineEvent.sender.full,
            Instant.fromEpochMilliseconds(timelineEvent.originTimestamp).toString(),
            content,
        ).asCsvLine()
    }

    private fun List<String>.asCsvLine() =
        joinToString(separator = properties.delimiter) { "\"" + it.replace("\"", "\"\"") + "\"" } +
                "\r\n"
}
