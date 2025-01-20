package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel.ReactionEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getTimelineEventReactionAggregation
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.originalName
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.RedactedEventContent


// TODO: remove?
// TODO: should consider outbox to get immediate feedback
@OptIn(ExperimentalCoroutinesApi::class)
fun getMessageUserReactions(
    client: MatrixClient,
    timelineEventFlow: Flow<TimelineEvent>,
    initials: Initials,
    roomId: RoomId,
    eventId: EventId,
): Flow<Map<String, Set<ReactionEvent>>> =
    timelineEventFlow.flatMapLatest { timelineEvent ->
        when (timelineEvent.content?.getOrNull()) {
            is RedactedEventContent -> flowOf(emptyMap())
            else -> client.room.getTimelineEventReactionAggregation(roomId, eventId)
                .flatMapLatest { reactions ->
                    combine(reactions.reactions.flatMap { (_, timelineEvents) ->
                        timelineEvents.map { timelineEvent ->
                            client.user.getById(roomId, timelineEvent.sender)
                        }
                    }) { users ->
                        reactions.reactions.mapValues { (_, events) ->
                            events.mapNotNull { event ->
                                users.find { it?.userId == event.sender }?.let { sender ->
                                    // TODO: refactor to be more general
                                    ReactionEvent(
                                        eventId = event.eventId,
                                        sender = UserInfoElement(
                                            name = sender.originalName ?: sender.name,
                                            userId = sender.userId,
                                            initials = initials.compute(sender.originalName ?: sender.name),
                                            image = null, // TODO
                                        ),
                                        isMe = event.sender == client.userId,
                                    )
                                }
                            }.toSet()
                        }
                    }
                }
        }
    }


@OptIn(ExperimentalCoroutinesApi::class)
fun getMessageUserReactions(
    client: MatrixClient,
    roomId: RoomId,
    eventId: EventId,
): Flow<MessageUserReactions> =
    client.room.getTimelineEventReactionAggregation(roomId, eventId)
        .flatMapLatest { reactions ->
            val reactionsAggregation = reactions.reactions
            val reactionCounts = mutableMapOf<ReactionKey, UInt>()
            val userReactions = mutableMapOf<UserId, MutableSet<ReactionKey>>()
            if (reactionsAggregation.isEmpty()) {
                return@flatMapLatest flowOf(MessageUserReactions())
            }
            reactionsAggregation.keys.forEach { reactionKey ->
                reactionsAggregation[reactionKey]?.let {
                    it.forEach { event ->
                        reactionCounts[reactionKey] = reactionCounts.getOrPut(reactionKey) { 0u } + 1u
                        userReactions.getOrPut(event.sender) { mutableSetOf() }
                            .add(reactionKey)
                    }
                }
            }
            combine(userReactions.keys.map { userId -> client.user.getById(roomId, userId) }) { users ->
                MessageUserReactions(
                    byUser = users.filterNotNull()
                        .associateWith { roomUser ->
                            userReactions.getOrElse(roomUser.userId) { setOf() }.toSet()
                        }.filterValues { it.isEmpty().not() },
                    byCount = reactionCounts,
                )
            }
        }

typealias ReactionKey = String

data class MessageUserReactions(
    val byUser: Map<RoomUser, Set<ReactionKey>> = emptyMap(),
    val byCount: Map<ReactionKey, UInt> = emptyMap(),
)
