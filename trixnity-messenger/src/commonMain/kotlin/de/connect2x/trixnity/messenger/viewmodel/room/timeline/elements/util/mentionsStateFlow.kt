package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.viewmodel.EventInfoElement
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toEventInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toRoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.Mention
import net.folivo.trixnity.core.model.Mention as CoreMention
import net.folivo.trixnity.core.model.RoomId

private val log = KotlinLogging.logger {}

@OptIn(ExperimentalCoroutinesApi::class)
fun mentionsStateFlow(
    content: String,
    roomId: RoomId,
    matrixClient: MatrixClient,
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

                is CoreMention.Room -> parseRoom(mention, matrixClient)
                    .map {
                        MessageMention.Room(it)
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                is CoreMention.RoomAlias -> parseRoom(mention, matrixClient)
                    .map {
                        MessageMention.Room(it)
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                is Mention.RoomAliasEvent ->
                    parseRoom(Mention.RoomAlias(mention.roomAliasId), matrixClient).filterNotNull().let { roomflow ->
                        roomflow.map { room ->
                            matrixClient.room.getTimelineEvent(
                                room.roomId, mention.eventId
                            ).map { event ->
                                event?.let {
                                    MessageMention.Event(
                                        it.toEventInfoElement(),
                                        roomflow
                                            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
                                    )
                                }
                            }
                        }.flattenMerge(3)
                    }
                        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                is Mention.RoomEvent -> matrixClient.room.getTimelineEvent(mention.roomId, mention.eventId)
                    .map { event ->
                        event?.let {
                            MessageMention.Event(
                                it.toEventInfoElement(),
                                parseRoom(Mention.Room(mention.roomId), matrixClient)
                                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
                            )
                        }
                    }
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
            }
        }

private fun parseRoom(roomMention: CoreMention, matrixClient: MatrixClient): Flow<RoomInfoElement> {
    return when (roomMention) {
        is CoreMention.Room -> matrixClient.room.getById(roomMention.roomId)
            .filterNotNull()
            .map {
                it.toRoomInfoElement(matrixClient)
            }

        is CoreMention.RoomAlias -> matrixClient.room.getAll()
            .map { rooms ->
                rooms.map { room ->
                    room.value
                        .filter {
                            it?.name?.explicitName == roomMention.roomAliasId.full
                        }
                        .filterNotNull()
                        .first()
                }.first().toRoomInfoElement(matrixClient)
            }
            .filterNotNull()

        is CoreMention.RoomEvent, is CoreMention.User, is CoreMention.RoomAliasEvent -> {
            log.warn { "Expected Room or RoomAlias, got User or Event" }
            flowOf(RoomInfoElement("", RoomId(""), "", null))
        }
    }
}

sealed interface MessageMention {
    data class Room(val room: RoomInfoElement) : MessageMention
    data class User(val user: UserInfoElement) : MessageMention
    data class Event(val event: EventInfoElement, val room: StateFlow<RoomInfoElement?>) : MessageMention
}
