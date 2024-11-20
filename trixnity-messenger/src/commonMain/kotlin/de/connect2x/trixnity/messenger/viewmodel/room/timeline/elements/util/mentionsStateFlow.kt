package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.EventInfoElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toRoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
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
import org.koin.core.Koin
import org.koin.core.component.get
import net.folivo.trixnity.core.model.Mention as CoreMention

@OptIn(ExperimentalCoroutinesApi::class)
fun MatrixClientViewModelContext.mentionsStateFlow(
    content: String,
    roomId: RoomId,
): Map<IntRange, StateFlow<MessageMention?>> =
    MatrixRegex.findMentions(content)
        .mapValues { (_, mention) ->
            when (mention) {
                is CoreMention.User -> matrixClient.user.getById(roomId, mention.userId)
                    .map {
                        MessageMention.User(
                            it?.toUserInfoElement(matrixClient)
                            // TODO call api.user.getProfile as fallback
                                ?: UserInfoElement(mention.userId.full, mention.userId)
                        )
                    }
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                is CoreMention.Room -> parseRoom(get(), matrixClient, mention.roomId)
                    .map { info ->
                        info?.let { MessageMention.Room(info) }
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                is CoreMention.RoomAlias ->
                    flow {
                        emitAll(
                            parseRoom(get(), matrixClient, mention.roomAliasId)
                                .map { info ->
                                    info?.let { MessageMention.Room(info) }
                                }
                        )
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                is Mention.Event -> parseRoom(get(), matrixClient, mention.roomId ?: roomId)
                    .flatMapLatest { roomInfo ->
                        if (roomInfo == null) flowOf(null)
                        else flowOf(MessageMention.Event(EventInfoElement(mention.eventId), roomInfo))
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
            }
        }

private fun parseRoom(
    config: MatrixMessengerConfiguration,
    matrixClient: MatrixClient,
    roomId: RoomId,
    forceAlias: RoomAliasId? = null
): Flow<RoomInfoElement?> =
    combine(
        matrixClient.room.getById(roomId),
        matrixClient.room.getState<CanonicalAliasEventContent>(roomId).map { it?.content },
    ) { room, aliases ->
        room?.toRoomInfoElement(
            config,
            matrixClient,
            forceAlias?.full ?: aliases?.alias?.full ?: aliases?.aliases?.firstOrNull()?.full ?: room.roomId.full
        )
    }

private suspend fun parseRoom(
    config: MatrixMessengerConfiguration,
    matrixClient: MatrixClient,
    roomAliasId: RoomAliasId,
): Flow<RoomInfoElement?> {
    val foundRoomId = matrixClient.room.getAll().first()
        .firstNotNullOfOrNull { (roomId, _) ->
            val aliasEvent = matrixClient.room.getState<CanonicalAliasEventContent>(roomId).first()?.content
                ?: return@firstNotNullOfOrNull null

            if (aliasEvent.alias == roomAliasId || aliasEvent.aliases?.contains(roomAliasId) == true) roomId
            else null
        } ?: return flowOf(null)
    return parseRoom(config, matrixClient, foundRoomId, roomAliasId)
}

sealed interface MessageMention {
    data class Room(val room: RoomInfoElement) : MessageMention
    data class User(val user: UserInfoElement) : MessageMention
    data class Event(val event: EventInfoElement, val room: RoomInfoElement) : MessageMention
}
