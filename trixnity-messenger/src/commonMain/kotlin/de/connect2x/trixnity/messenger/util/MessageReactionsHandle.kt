package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.MessageUserReactions.ReactionEvent
import de.connect2x.trixnity.messenger.util.MessageUserReactions.UserReactions
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.debounceAfterFirst
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getTimelineEventReactionAggregation
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.relatesTo
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.RelationType.Annotation
import org.koin.core.component.get
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


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
    fun getReactions(): Flow<MessageUserReactions>
}

class MessageReactionsHandleImpl(
    private val roomId: RoomId,
    private val eventId: EventId,
    private val initials: Initials,
    private val client: MatrixClient,
    private val config: MatrixMessengerConfiguration,
    private val scope: CoroutineScope,
) : MessageReactionsHandle {
    override fun getReactions(): Flow<MessageUserReactions> =
//        getMessageReactionsContinuousFlow(
//            roomId = roomId,
//            eventId = eventId,
//            initials = initials,
//            client = client,
//            config = config,
//            scope = scope,
//        )
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
    val byReaction: Map<ReactionKey, Set<ReactionEvent>>,
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
                            isByMe = userId == timelineEvent.sender,
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
            )
        }

private fun getMessageReactionsContinuousFlow(
    roomId: RoomId,
    eventId: EventId,
    initials: Initials,
    client: MatrixClient,
    config: MatrixMessengerConfiguration,
    scope: CoroutineScope,
): Flow<MessageUserReactions> = client
    .getReactions(roomId, eventId)
    .debounceAfterFirst(125.milliseconds)
    .map {
        val byUser = mutableMapOf<UserId, Pair<StateFlow<UserInfoElement?>, MutableSet<ReactionKey>>>()
        val byReaction = it.map { (reactionKey, userEvents) ->
            reactionKey to userEvents.map { (userId, _) ->
                val userInfo = client.user.getById(roomId, userId)
                    .map { roomUser ->
                        roomUser.toUserInfoElement(
                            coroutineScope = scope,
                            matrixClient = client,
                            initials = initials,
                            maxAvatarSize = config.avatarMaxSize,
                            fallbackUserId = userId,
                        )
                    }
                    .stateIn(scope, whileSubscribedWithTimeout, null)

                byUser.getOrPut(userId) { Pair(userInfo, mutableSetOf()) }
                    .second.add(reactionKey)

                ReactionEvent(
                    eventId = eventId,
                    userInfo = userInfo,
                    isByMe = userId == client.userId,
                )
            }.toSet()
        }.toMap()
        MessageUserReactions(
            byUser = byUser.mapValues { (_, info) ->
                UserReactions(
                    userInfo = info.first,
                    reactions = info.second.toSet(),
                )
            },
            byReaction = byReaction,
        )
    }

@OptIn(ExperimentalCoroutinesApi::class)
private fun MatrixClient.getReactions(roomId: RoomId, eventId: EventId) =
    room.getTimelineEventRelations(roomId, eventId, Annotation)
        .flatMapLatest { reactionMap ->
            if (reactionMap.isNullOrEmpty()) flowOf(emptyList())
            else combine(reactionMap.values) {
                it.mapNotNull { reaction -> reaction?.eventId }
            }
        }
        .flatMapLatest { reactions ->
            channelFlow<Map<ReactionKey, Map<UserId, TimelineEvent>>> {
                send(mapOf())
                // TODO: make thread safe
                val aggregation = mutableMapOf<ReactionKey, MutableMap<UserId, TimelineEvent>>()
                reactions.forEach {
                    // TODO: make concurrent
//                    launch {
                    withTimeoutOrNull(10.seconds) {
                        room.getTimelineEvent(roomId, it).first()
                    }?.let { event ->
                        val relatesTo = event.relatesTo
                        if (relatesTo is RelatesTo.Annotation) {
                            relatesTo.key?.let { reactionKey ->
                                aggregation.getOrPut(reactionKey) { mutableMapOf() }.also {
                                    val previous = it[event.sender]
                                    if (previous == null || previous.originTimestamp < event.originTimestamp) {
                                        it[event.sender] = event
                                        send(aggregation)
                                    }
                                }
                            }
                        }
                    }
//                    }
                }
            }
        }

typealias ReactionKey = String
