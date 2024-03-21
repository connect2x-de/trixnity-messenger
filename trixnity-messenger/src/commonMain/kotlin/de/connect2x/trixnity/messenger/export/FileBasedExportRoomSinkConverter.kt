package de.connect2x.trixnity.messenger.export

import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.RoomId

interface FileBasedExportRoomSinkConverterFactory {
    /**
     * @return null, if [properties] is not supported
     */
    fun create(roomId: RoomId, properties: FileBasedExportRoomProperties): FileBasedExportRoomSinkConverter?
}

interface FileBasedExportRoomSinkConverter {
    val extension: String

    /**
     * @return an optional starting string (for example the header of a CSV file).
     */
    suspend fun prefix(): String? = null

    /**
     * @return an optional ending string (for example the closing tag of an XML file).
     */
    suspend fun suffix(): String? = null

    /**
     * Convert a timeline event to new content in a file (plaintext, CSV, JSON, etc.)
     */
    suspend fun convert(timelineEvent: TimelineEvent, filename: String?): String?
}
