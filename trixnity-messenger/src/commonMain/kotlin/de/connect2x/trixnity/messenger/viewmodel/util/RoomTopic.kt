package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.TopicEventContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface RoomTopic {
    fun getRoomTopic(
        roomId: RoomId,
        matrixClient: MatrixClient,
        formatted: Boolean = true,
    ): Flow<String>
}

open class RoomTopicImpl : RoomTopic {
    override fun getRoomTopic(
        roomId: RoomId,
        matrixClient: MatrixClient,
        formatted: Boolean,
    ): Flow<String> = matrixClient.room
        .getState<TopicEventContent>(roomId = roomId, stateKey = "")
        .map { topic ->
            val content = topic?.content
            content?.topic?.text?.plain ?: content?.legacy?.topic ?: ""
        }
}
