package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.TopicEventContent


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
            topic?.content?.topic ?: ""
        }
}
