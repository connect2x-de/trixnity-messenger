package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.MessageUserReactions.ReactionEvent
import de.connect2x.trixnity.messenger.viewmodel.util.MessageUserReactions.ReactionEventCollection
import de.connect2x.trixnity.messenger.viewmodel.util.MessageUserReactions.UserReactions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getTimelineEventReactionAggregation
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get
import kotlin.time.Duration.Companion.milliseconds


interface MessageReactionsHandleFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId,
        eventId: EventId,
    ): MessageReactionsHandle =
        MessageReactionsHandleImpl(
            roomId = roomId,
            eventId = eventId,
            initials = viewModelContext.get<Initials>(),
            client = viewModelContext.matrixClient,
            config = viewModelContext.get<MatrixMessengerConfiguration>(),
            scope = viewModelContext.coroutineScope,
        )

    companion object : MessageReactionsHandleFactory
}

interface MessageReactionsHandle {
    val reactions: Flow<MessageUserReactions>
}

class MessageReactionsHandleImpl(
    roomId: RoomId,
    eventId: EventId,
    initials: Initials,
    client: MatrixClient,
    config: MatrixMessengerConfiguration,
    scope: CoroutineScope,
) : MessageReactionsHandle {
    override val reactions =
        client
            .getMessageUserReactions(
                roomId = roomId,
                eventId = eventId,
                initials = initials,
                config = config,
                scope = scope,
            )
}

data class MessageUserReactions(
    val byUser: Map<UserId, UserReactions>,
    val byReaction: Map<ReactionKey, ReactionEventCollection>,
) {
    data class UserReactions(
        val userInfo: StateFlow<UserInfoElement?>,
        val reactions: Set<ReactionKey>,
    )

    data class ReactionEvent(
        val eventId: EventId,
        val userInfo: StateFlow<UserInfoElement?>,
        val isByMe: Boolean,
    )

    class ReactionEventCollection(
        reactionEvents: Set<ReactionEvent>,
        scope: CoroutineScope,
    ) : Set<ReactionEvent> by reactionEvents {
        val flattenUserInfos: StateFlow<Set<UserInfoElement>> =
            combine(this.map {
                it.userInfo
            }) { it.filterNotNull() }
                .map { it.toSet() }
                .debounceAfterFirst(250.milliseconds)
                .stateIn(scope, whileSubscribedWithTimeout, setOf())
    }

    companion object {
        val Empty = MessageUserReactions(mapOf(), mapOf())
    }
}

// TODO: should consider outbox to get immediate feedback
@OptIn(ExperimentalCoroutinesApi::class)
fun MatrixClient.getMessageUserReactions(
    roomId: RoomId,
    eventId: EventId,
    initials: Initials,
    config: MatrixMessengerConfiguration,
    scope: CoroutineScope,
): Flow<MessageUserReactions> =
    room
        .getTimelineEventReactionAggregation(roomId, eventId)
        .mapLatest { reactions ->
            val reactionsAggregation = reactions.reactions
            if (reactionsAggregation.isEmpty()) {
                return@mapLatest MessageUserReactions.Empty
            }
            val byUser = mutableMapOf<UserId, Pair<StateFlow<UserInfoElement?>, MutableSet<ReactionKey>>>()
            val byReaction: Map<ReactionKey, Set<ReactionEvent>> = reactionsAggregation
                .mapValues { (reactionKey, events) ->
                    events.map { timelineEvent ->
                        val userId = timelineEvent.sender
                        val userInfo = user
                            .getById(timelineEvent.roomId, userId)
                            .map { roomUser ->
                                roomUser.toUserInfoElement(
                                    coroutineScope = scope,
                                    matrixClient = this,
                                    initials = initials,
                                    maxAvatarSize = config.avatarMaxSize,
                                    fallbackUserId = userId,
                                )
                            }
                            .stateIn(scope, whileSubscribedWithTimeout, null)

                        byUser.getOrPut(userId) { Pair(userInfo, mutableSetOf()) }
                            .second.add(reactionKey)

                        ReactionEvent(
                            eventId = timelineEvent.eventId,
                            userInfo = userInfo,
                            isByMe = this.userId == timelineEvent.sender,
                        )
                    }.toSet()
                }
            MessageUserReactions(
                byUser = byUser.mapValues { (_, info) ->
                    UserReactions(
                        userInfo = info.first,
                        reactions = info.second.toSet(),
                    )
                },
                byReaction = byReaction
                    .mapValues { ReactionEventCollection(it.value, scope) }
            )
        }

typealias ReactionKey = String
