package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.EventInfoElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.toRoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
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
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import net.folivo.trixnity.core.model.events.m.room.formattedBodyWithoutFallback
import org.koin.core.component.get

abstract class TextBasedRoomMessageTimelineElementViewModel<C : RoomMessageEventContent.TextBased>(
    private val viewModelContext: MatrixClientViewModelContext,
    content: C,
    private val roomId: RoomId,
    private val onOpenMention: OpenMentionCallback,
) : RoomMessageTimelineElementViewModel.TextBased<C>, MatrixClientViewModelContext by viewModelContext {
    private val initials = get<Initials>()
    private val config = get<MatrixMessengerConfiguration>()
    override val body: String = content.bodyWithoutFallback
    override val formattedBody: String? = content.formattedBodyWithoutFallback

    override val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention?>> by lazy {
        findMentions(body)
    }

    override val mentionsInFormattedBody: Map<IntRange, StateFlow<TimelineElementMention?>>? by lazy {
        formattedBody?.let(::findMentions)
    }

    override fun openMention(mention: TimelineElementMention) {
        onOpenMention(userId, mention)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun findMentions(text: String) =
        MatrixRegex.findMentions(text)
            .mapValues { (_, mention) ->
                when (mention) {
                    is Mention.User -> matrixClient.user.getById(roomId, mention.userId)
                        .map {
                            TimelineElementMention.User(
                                it?.toUserInfoElement(matrixClient, initials, config.avatarMaxSize)
                                // TODO call api.user.getProfile as fallback
                                    ?: UserInfoElement(mention.userId.full, mention.userId)
                            )
                        }
                        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                    is Mention.Room -> parseRoom(mention.roomId, matrixClient, initials)
                        .map { info ->
                            info?.let { TimelineElementMention.Room(info) }
                        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                    is Mention.RoomAlias ->
                        flow {
                            emitAll(
                                parseRoom(mention.roomAliasId, matrixClient, initials)
                                    .map { info ->
                                        info?.let { TimelineElementMention.Room(info) }
                                    }
                            )
                        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                    is Mention.Event -> parseRoom(mention.roomId ?: roomId, matrixClient, initials)
                        .flatMapLatest { roomInfo ->
                            if (roomInfo == null) flowOf(null)
                            else flowOf(TimelineElementMention.Event(EventInfoElement(mention.eventId), roomInfo))
                        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
                }
            }

    private fun parseRoom(
        roomId: RoomId,
        matrixClient: MatrixClient,
        initials: Initials,
        forceAlias: RoomAliasId? = null
    ): Flow<RoomInfoElement?> =
        combine(
            matrixClient.room.getById(roomId),
            matrixClient.room.getState<CanonicalAliasEventContent>(roomId).map { it?.content },
        ) { room, aliases ->
            room?.toRoomInfoElement(
                matrixClient,
                forceAlias?.full ?: aliases?.alias?.full ?: aliases?.aliases?.firstOrNull()?.full ?: room.roomId.full,
                initials,
            )
        }

    private suspend fun parseRoom(
        roomAliasId: RoomAliasId,
        matrixClient: MatrixClient,
        initials: Initials,
    ): Flow<RoomInfoElement?> {
        val foundRoomId = matrixClient.room.getAll().first()
            .firstNotNullOfOrNull { (roomId, _) ->
                val aliasEvent = matrixClient.room.getState<CanonicalAliasEventContent>(roomId).first()?.content
                    ?: return@firstNotNullOfOrNull null

                if (aliasEvent.alias == roomAliasId || aliasEvent.aliases?.contains(roomAliasId) == true) roomId
                else null
            } ?: return flowOf(null)
        return parseRoom(foundRoomId, matrixClient, initials, roomAliasId)
    }

}
