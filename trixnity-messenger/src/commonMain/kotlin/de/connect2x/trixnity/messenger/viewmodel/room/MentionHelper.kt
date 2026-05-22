package de.connect2x.trixnity.messenger.viewmodel.room

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.model.RoomAliasId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import de.connect2x.trixnity.core.util.Reference
import de.connect2x.trixnity.messenger.util.MatrixReferences
import de.connect2x.trixnity.messenger.util.html.HtmlNode
import de.connect2x.trixnity.messenger.viewmodel.EventInfoElement
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.toRoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MentionHelper(
    val coroutineScope: CoroutineScope,
    val matrixClient: MatrixClient,
    val roomId: RoomId,
    val initials: Initials,
    val roomName: RoomName,
    val maxMediaSizeInMemory: Long,
) {
    fun processMentions(content: HtmlNode.HtmlElement): Flow<Map<String, TimelineElementMention?>> =
        content
            .let(MatrixReferences::findInHtml)
            .mapValues { (_, mention) -> processMention(mention) }
            .map { (key, flow) -> flow.map { Pair(key, it) } }
            .let { combine(it, Array<Pair<String, TimelineElementMention?>>::toMap) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun processMention(reference: Reference): StateFlow<TimelineElementMention?> =
        when (reference) {
            is Reference.User ->
                matrixClient.user
                    .getById(roomId, reference.userId)
                    .map {
                        TimelineElementMention.User(
                            // TODO call api.user.getProfile as fallback
                            it.toUserInfoElement(
                                coroutineScope,
                                matrixClient,
                                initials,
                                reference.userId,
                                maxMediaSizeInMemory,
                            )
                        )
                    }
                    .stateIn(coroutineScope, whileSubscribedWithTimeout, null)

            is Reference.Room ->
                parseRoom(reference.roomId, matrixClient, initials)
                    .map { info -> TimelineElementMention.Room(info) }
                    .stateIn(coroutineScope, whileSubscribedWithTimeout, null)

            is Reference.RoomAlias ->
                flow {
                        emitAll(
                            parseRoom(reference.roomAliasId, matrixClient, initials).map { info ->
                                info?.let { TimelineElementMention.Room(info) }
                            }
                        )
                    }
                    .stateIn(coroutineScope, whileSubscribedWithTimeout, null)

            is Reference.Event ->
                parseRoom(reference.roomId ?: roomId, matrixClient, initials)
                    .map { roomInfo -> TimelineElementMention.Event(EventInfoElement(reference.eventId), roomInfo) }
                    .stateIn(coroutineScope, whileSubscribedWithTimeout, null)

            is Reference.Link -> MutableStateFlow(null)
        }

    private fun parseRoom(
        roomId: RoomId,
        matrixClient: MatrixClient,
        initials: Initials,
        forceAlias: RoomAliasId? = null,
    ): Flow<RoomInfoElement> =
        combine(
            matrixClient.room.getById(roomId),
            roomName.getRoomName(roomId, matrixClient),
            matrixClient.room.getState<CanonicalAliasEventContent>(roomId).map { it?.content },
        ) { room, roomName, _ ->
            room?.toRoomInfoElement(
                coroutineScope,
                initials,
                matrixClient,
                forceAlias?.full ?: roomName,
                maxMediaSizeInMemory,
            )
                ?: forceAlias?.let { alias ->
                    RoomInfoElement(
                        name = forceAlias.full,
                        roomId = roomId,
                        roomImageInitials = initials.compute(forceAlias.full),
                        roomImage = null,
                    )
                }
                ?: RoomInfoElement(roomName, roomId, initials.compute(roomName), null)
        }

    private suspend fun findRoomAlias(roomAliasId: RoomAliasId): RoomId? =
        matrixClient.room.getAll().first().firstNotNullOfOrNull { (roomId, _) ->
            val aliasEvent =
                matrixClient.room.getState<CanonicalAliasEventContent>(roomId).first()?.content
                    ?: return@firstNotNullOfOrNull null

            if (aliasEvent.alias == roomAliasId || aliasEvent.aliases?.contains(roomAliasId) == true) roomId else null
        }

    private suspend fun lookupRoomAlias(roomAliasId: RoomAliasId): RoomId? =
        matrixClient.api.room.getRoomAlias(roomAliasId).getOrNull()?.roomId

    private suspend fun parseRoom(
        roomAliasId: RoomAliasId,
        matrixClient: MatrixClient,
        initials: Initials,
    ): Flow<RoomInfoElement?> {
        val roomId = findRoomAlias(roomAliasId) ?: lookupRoomAlias(roomAliasId) ?: return flowOf(null)
        return parseRoom(roomId, matrixClient, initials, roomAliasId)
    }
}
