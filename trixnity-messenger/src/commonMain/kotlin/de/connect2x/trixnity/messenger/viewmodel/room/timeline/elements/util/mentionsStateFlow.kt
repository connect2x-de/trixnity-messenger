package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.viewmodel.EventInfoElement
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toEventInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toRoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.Mention
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.Mention as CoreMention

@OptIn(ExperimentalCoroutinesApi::class)
fun mentionsStateFlow(
    content: String,
    roomId: RoomId,
    matrixClient: MatrixClient,
    roomName: RoomName,
    coroutineScope: CoroutineScope
): Map<String, StateFlow<MessageMention?>> =
    MatrixRegex.findMentions(content)
        .mapValues { (_, mention) ->
            when (mention) {
                is CoreMention.User -> matrixClient.user.getById(roomId, mention.userId)
                    .filterNotNull()
                    .map {
                        MessageMention.User(it.toUserInfoElement(matrixClient))
                    }
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                is CoreMention.Room -> parseRoom(mention.roomId, matrixClient, roomName)
                    .map { info ->
                        info?.let { MessageMention.Room(info) }
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                is CoreMention.RoomAlias ->
                    flow {
                        emitAll(
                            parseRoom(mention.roomAliasId, matrixClient, roomName)
                                .map { info ->
                                    info?.let { MessageMention.Room(info) }
                                }
                        )
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                is Mention.RoomAliasEvent ->
                    flow {
                        emitAll(
                            parseRoom(mention.roomAliasId, matrixClient, roomName).flatMapLatest { roomInfo ->
                                if (roomInfo == null) flowOf(null)
                                else matrixClient.room.getTimelineEvent(roomInfo.roomId, mention.eventId)
                                    .map { event ->
                                        event?.let { MessageMention.Event(it.toEventInfoElement(), roomInfo) }
                                    }
                            }
                        )
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                is Mention.RoomEvent ->
                    parseRoom(mention.roomId, matrixClient, roomName).flatMapLatest { roomInfo ->
                        if (roomInfo == null) flowOf(null)
                        else matrixClient.room.getTimelineEvent(roomInfo.roomId, mention.eventId)
                            .map { event ->
                                event?.let { MessageMention.Event(it.toEventInfoElement(), roomInfo) }
                            }
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                is Mention.Event -> parseRoom(roomId, matrixClient, roomName).flatMapLatest { roomInfo ->
                    if (roomInfo == null) flowOf(null)
                    else matrixClient.room.getTimelineEvent(roomInfo.roomId, mention.eventId)
                        .map { event ->
                            event?.let { MessageMention.Event(it.toEventInfoElement(), roomInfo) }
                        }
                }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
            }
        }

@OptIn(ExperimentalCoroutinesApi::class)
private fun parseRoom(roomId: RoomId, matrixClient: MatrixClient, roomName: RoomName): Flow<RoomInfoElement?> =
    matrixClient.room.getById(roomId)
        .flatMapLatest { room ->
            if (room == null) flowOf(null)
            else roomName.getRoomName(room, matrixClient).map {
                room.toRoomInfoElement(matrixClient, it)
            }
        }

private suspend fun parseRoom(
    roomAliasId: RoomAliasId,
    matrixClient: MatrixClient,
    roomName: RoomName
): Flow<RoomInfoElement?> {
    val foundRoomId = matrixClient.room.getAll().first()
        .firstNotNullOfOrNull { (roomId, _) ->
            val aliasEvent = matrixClient.room.getState<CanonicalAliasEventContent>(roomId).first()?.content
                ?: return@firstNotNullOfOrNull null

            if (aliasEvent.alias == roomAliasId || aliasEvent.aliases?.contains(roomAliasId) == true) roomId
            else null
        } ?: return flowOf(null)
    return parseRoom(foundRoomId, matrixClient, roomName)
}

sealed interface MessageMention {
    data class Room(val room: RoomInfoElement) : MessageMention
    data class User(val user: UserInfoElement) : MessageMention
    data class Event(val event: EventInfoElement, val room: RoomInfoElement) : MessageMention
}
