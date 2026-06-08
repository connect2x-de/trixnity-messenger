package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getTimelineEventReactionAggregation
import de.connect2x.trixnity.client.store.eventId
import de.connect2x.trixnity.client.store.sender
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.RedactedEventContent
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

interface GetEventReactions {
    operator fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId,
        eventId: EventId,
        initials: Initials,
        maxMediaSizeInMemory: Long,
    ): Flow<EventReactions>
}

// TODO: should consider outbox (react and redact) to get immediate feedback
class GetEventReactionsImpl : GetEventReactions {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId,
        eventId: EventId,
        initials: Initials,
        maxMediaSizeInMemory: Long,
    ): Flow<EventReactions> =
        matrixClient.room.getTimelineEvent(roomId, eventId).flatMapLatest { timelineEvent ->
            when (timelineEvent?.content?.getOrNull()) {
                null,
                is RedactedEventContent -> flowOf(EventReactions(emptySet()))
                else ->
                    matrixClient.room.getTimelineEventReactionAggregation(roomId, eventId).scopedFlatMapLatest {
                        reactions ->
                        if (
                            reactions.reactions.isEmpty()
                        ) { // we have to return early here as otherwise we will not get a value of the combine()
                            flowOf(EventReactions(emptySet()))
                        } else {
                            combine(
                                reactions.reactions.flatMap { (_, timelineEvents) ->
                                    timelineEvents
                                        .map { it.sender }
                                        .toSet()
                                        .map { userId -> matrixClient.user.getById(roomId, userId) }
                                }
                            ) { users ->
                                val mappedUsers = users.filterNotNull().associateBy { it.userId }
                                reactions.reactions.entries
                                    .flatMap { (value, events) ->
                                        events.mapNotNull { event ->
                                            mappedUsers[event.sender]?.let { sender ->
                                                EventReaction(
                                                    value = value,
                                                    eventId = event.eventId,
                                                    sender =
                                                        sender.toUserInfoElement(
                                                            this,
                                                            matrixClient,
                                                            initials,
                                                            maxMediaSizeInMemory,
                                                        ),
                                                    isByMe = event.sender == matrixClient.userId,
                                                )
                                            }
                                        }
                                    }
                                    .toSet()
                                    .let(::EventReactions)
                            }
                        }
                    }
            }
        }
}

data class EventReaction(val value: String, val sender: UserInfoElement, val eventId: EventId, val isByMe: Boolean)

data class EventReactions(val all: Set<EventReaction>) {
    val byUser: Map<UserId, ByUserInfo> by lazy {
        all.groupBy { it.sender.userId }
            .mapValues { (_, value) ->
                val first = value.first()
                ByUserInfo(
                    reactions = value.associate { it.value to it.eventId },
                    sender = first.sender,
                    isMe = first.isByMe,
                )
            }
    }
    val byReaction: Map<String, Set<ByReactionInfo>> by lazy {
        all.groupBy { it.value }
            .mapValues { (_, value) ->
                value.map { ByReactionInfo(eventId = it.eventId, sender = it.sender, isMe = it.isByMe) }.toSet()
            }
    }

    data class ByUserInfo(val reactions: Map<String, EventId>, val sender: UserInfoElement, val isMe: Boolean)

    data class ByReactionInfo(val eventId: EventId, val sender: UserInfoElement, val isMe: Boolean)

    companion object {
        val Empty = EventReactions(setOf())
    }
}
