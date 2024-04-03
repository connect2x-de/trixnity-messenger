package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.Mention
import net.folivo.trixnity.core.model.Mention as CoreMention
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.client.store.Room as RoomStore

private val log = KotlinLogging.logger {}

fun mentionsStateFlow(
    content: String,
    roomId: RoomId,
    matrixClient: MatrixClient,
    coroutineScope: CoroutineScope
): Map<String, StateFlow<MessageMention>> =
    MatrixRegex.findMentions(content)
        .mapValues { (_, mention) ->
            when (mention) {
                is CoreMention.User -> matrixClient.user.getById(roomId, mention.userId)
                    .filterNotNull()
                    .map {
                        MessageMention.User(it.toUserInfoElement(matrixClient))
                    }
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), MessageMention.User(UserInfoElement("", UserId(""))))

                is CoreMention.Room -> parseRoom(mention, matrixClient)
                    .map {
                        MessageMention.Room(it)
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), MessageMention.Room(RoomStore(RoomId(""))))

                is CoreMention.RoomAlias -> parseRoom(mention, matrixClient)
                    .map {
                        MessageMention.Room(it)
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), MessageMention.Room(RoomStore(RoomId(""))))

                is CoreMention.Event -> MutableStateFlow(MessageMention.Event(mention.eventId,
                    parseRoom(mention.room, matrixClient)
                        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), RoomStore(RoomId("")))
                )).stateIn(coroutineScope, SharingStarted.WhileSubscribed(), MessageMention.Event(EventId(""), MutableStateFlow(RoomStore(RoomId("")))))

                is CoreMention.Unknown -> {
                    log.error { "Unknown mention type" }
                    MutableStateFlow(MessageMention.Unknown(Unit))
                        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), MessageMention.Unknown(Unit))
                }
            }
        }

private fun parseRoom(roomMention: CoreMention, matrixClient: MatrixClient): Flow<RoomStore> {
    return when (roomMention) {
        is CoreMention.Room -> matrixClient.room.getById(roomMention.roomId)
            .filterNotNull()
            .map {
                it
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
                    }.first()
            }
            .filterNotNull()

        is CoreMention.Event, is CoreMention.User, is CoreMention.Unknown -> {
            log.error { "Expected Room or RoomAlias, got User or Event" }
            flowOf(RoomStore(RoomId("")))
        }
    }
}

sealed interface MessageMention {
    data class Room(val room: RoomStore) : MessageMention
    data class User(val user: UserInfoElement) : MessageMention
    data class Event(val event: EventId, val room: StateFlow<RoomStore>) : MessageMention
    data class Unknown(val nothing: Unit) : MessageMention
}
