package de.connect2x.trixnity.messenger.export

import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.utils.ByteArrayFlow

interface ExportRoomSinkProperties

interface ExportRoomSinkFactory {
    /**
     * @return null, if [properties] is not supported
     */
    fun create(roomId: RoomId, properties: ExportRoomSinkProperties): ExportRoomSink?
}

interface ExportRoomSink {
    class Media(
        val content: ByteArrayFlow,
        val fileName: String,
    )

    /**
     * Prepare the export (e.g. creating directories).
     */
    suspend fun start(): Result<Unit> = Result.success(Unit)

    /**
     * Complete the export (e.g. creating a ZIP).
     */
    suspend fun finish(): Result<Unit> = Result.success(Unit)

    /**
     * Process the event (e.g. write into file).
     */
    suspend fun processTimelineEvent(
        timelineEvent: TimelineEvent,
        media: Media? = null,
    ): Result<Unit>
}
