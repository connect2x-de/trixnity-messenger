package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.MessageUserReactions.ReactionEvent
import de.connect2x.trixnity.messenger.util.MessageUserReactions.UserReactions
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.relatesTo
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.RelationType.Annotation
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


//interface MessageReactionsHandleFactory {
//    fun create(
//        roomId: RoomId,
//        eventId: EventId,
//        senderId: UserId,
//        client: MatrixClient,
//        scope: CoroutineScope,
//    ): MessageReactionsHandle =
//        MessageReactionsHandleImpl()
//
//    companion object : MessageReactionsHandleFactory
//}

interface MessageReactionsHandleFactory {
    fun getMessageReactions(

    ): Flow<MessageUserReactions>
}

//val reactions: StateFlow<Map<ReactionKey, Set<ReactionEvent>>> =;

interface MessageReactionsHandle {
    val byUser: Map<UserId, UserReactions>
    val byReaction: Map<ReactionKey, Set<ReactionEvent>>
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

private fun getMessageReactionsStats(
    roomId: RoomId,
    eventId: EventId,
    initials: Initials,
    client: MatrixClient,
    config: MatrixMessengerConfiguration,
    scope: CoroutineScope,
) = client
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

//class MessageReactionsHandleImpl : MessageReactionsHandle {
//
//    // TODO: should consider outbox to get immediate feedback
//    @OptIn(ExperimentalCoroutinesApi::class)
//    fun getMessageUserReactions(
//        client: MatrixClient,
//        roomId: RoomId,
//        eventId: EventId,
//    ): Flow<MessageUserReactions> =
//        client.room.getTimelineEventReactionAggregation(roomId, eventId)
//            .mapLatest { reactions ->
//                val reactionsAggregation = reactions.reactions
//                if (reactionsAggregation.isEmpty()) {
//                    return@mapLatest MessageUserReactions.Empty
//                }
//                val byUser: MutableMap<UserId, MutableSet<ReactionKey>> = mutableMapOf()
//                val byCount: Map<ReactionKey, ReactionCount> = reactionsAggregation
//                    .mapValues { (key, events) ->
//                        events.forEach {
//                            byUser.getOrPut(it.sender) { mutableSetOf() }.add(key)
//                        }
//                        ReactionCount(events)
//                    }
//                MessageUserReactions(
//                    byUser = byUser.mapValues { (userId, reactionKeys) ->
//                        UserReactions(
//                            client.user.getById(roomId, userId),
//                            reactions = reactionKeys.toSet(),
//                        )
//                    },
//                    byCount = byCount,
//                )
//            }
//}

typealias ReactionKey = String
