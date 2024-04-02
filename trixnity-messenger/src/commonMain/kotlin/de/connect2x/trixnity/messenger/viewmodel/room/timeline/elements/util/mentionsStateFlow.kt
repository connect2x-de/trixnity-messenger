package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.Mention as CoreMention
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.client.store.Room as RoomStore

fun mentionsStateFlow(
    content: String,
    roomId: RoomId,
    matrixClient: MatrixClient,
    coroutineScope: CoroutineScope
): Map<String, StateFlow<Mention>> =
    MatrixRegex.findMentions(content)
        .mapValues { (_, mention) ->
            when (mention) {
                is CoreMention.User -> matrixClient.user.getById(roomId, mention.userId)
                    .filterNotNull()
                    .map {
                        Mention.User(it.toUserInfoElement(matrixClient))
                    }
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Mention.User(UserInfoElement("", UserId(""))))

                is CoreMention.Room -> matrixClient.room.getById(mention.roomId)
                    .filterNotNull()
                    .map {
                        Mention.Room(it)
                    }
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Mention.Room(Room(RoomId(""))))

                is CoreMention.RoomAlias -> matrixClient.room.getAll()
                    .map { rooms ->
                        Mention.Room(
                            rooms.map { room ->
                                room.value
                                    .filter {
                                        it?.name?.explicitName == mention.roomAliasId.full
                                    }
                                    .filterNotNull()
                                    .first()
                            }.first()
                        )
                    }
                    .filterNotNull()
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Mention.Room(Room(RoomId(""))))

                is CoreMention.Event -> MutableStateFlow(Mention.Event(mention.eventId))
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Mention.Event(EventId("")))
            }
        }

sealed interface Mention {
    data class Room(val room: RoomStore) : Mention
    data class User(val user: UserInfoElement) : Mention
    data class Event(val event: EventId) : Mention
}
