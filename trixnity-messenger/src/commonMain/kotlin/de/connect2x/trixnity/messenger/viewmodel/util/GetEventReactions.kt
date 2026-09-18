package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.flatten
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getTimelineEventReactionAggregation
import de.connect2x.trixnity.client.store.eventId
import de.connect2x.trixnity.client.store.sender
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.RedactedEventContent
import de.connect2x.trixnity.core.model.events.m.ReactionEventContent
import de.connect2x.trixnity.core.model.events.m.RelatesTo
import de.connect2x.trixnity.core.model.events.m.room.RedactionEventContent
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId.Companion.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

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
                else -> {
                    val timelineReactions =
                        matrixClient.room.getTimelineEventReactionAggregation(roomId, eventId).scopedFlatMapLatest {
                            reactions ->
                            println("Reactions are $reactions")
                            if (
                                reactions.reactions.isEmpty()
                            ) { // we have to return early here as otherwise we will not get a value of the combine()
                                flowOf(emptySet())
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
                                                        eventOrTransactionId = EventIdOrTransactionId(event.eventId),
                                                        sender =
                                                            sender.toUserInfoElement(
                                                                this,
                                                                matrixClient,
                                                                initials,
                                                                maxMediaSizeInMemory,
                                                            ),
                                                        isByMe = event.sender == matrixClient.userId,
                                                        isPending = false,
                                                    )
                                                }
                                            }
                                        }
                                        .toSet()
                                }
                            }
                        }
                    val outboxRedactions =
                        matrixClient.room.getOutbox(roomId).scopedFlatMapLatest {
                            if (it.isEmpty()) {
                                (flowOf(emptySet()))
                            } else {
                                combine(it) { outboxMessages ->
                                    outboxMessages
                                        .mapNotNull { outboxMessage ->
                                            val content = outboxMessage?.content
                                            if (content is RedactionEventContent) {
                                                content.redacts
                                            } else {
                                                null
                                            }
                                        }
                                        .toSet()
                                }
                            }
                        }

                    val outboxReactions =
                        matrixClient.user
                            .getById(roomId, matrixClient.userId)
                            .filterNotNull()
                            .flatMapLatest { user ->
                                matrixClient.room.getOutbox(roomId).scopedFlatMapLatest {
                                    if (it.isEmpty()) {
                                        (flowOf(emptyList()))
                                    } else {
                                        combine(it) { outboxMessages ->
                                            outboxMessages
                                                .mapNotNull { outboxMessage ->
                                                    if (outboxMessage != null && outboxMessage.sendError == null) {
                                                        val relatesTo = outboxMessage.content.relatesTo
                                                        if (
                                                            relatesTo is RelatesTo.Annotation &&
                                                                relatesTo.eventId == eventId &&
                                                                outboxMessage.content is ReactionEventContent
                                                        ) {
                                                            relatesTo.key?.let { it to outboxMessage }
                                                        } else {
                                                            null
                                                        }
                                                    } else {
                                                        null
                                                    }
                                                }
                                                .groupBy { (reaction, _) -> reaction }
                                                .mapValues { (_, keyToEvents) ->
                                                    keyToEvents.map { (_, event) -> event }.last()
                                                }
                                                .map { outboxEvent ->
                                                    // We need to check whether the event has been redacted while still
                                                    // part of the outbox
                                                    val eventId = outboxEvent.value.eventId
                                                    if (eventId != null) {
                                                        matrixClient.room.getTimelineEvent(roomId, eventId).map {
                                                            if (it?.content?.getOrNull() is RedactedEventContent) {
                                                                null
                                                            } else
                                                                EventReaction(
                                                                    value = outboxEvent.key,
                                                                    eventOrTransactionId =
                                                                        outboxEvent.value.eventId?.let { id ->
                                                                            EventIdOrTransactionId(id)
                                                                        }
                                                                            ?: EventIdOrTransactionId(
                                                                                outboxEvent.value.transactionId
                                                                            ),
                                                                    sender =
                                                                        user.toUserInfoElement(
                                                                            this,
                                                                            matrixClient,
                                                                            initials,
                                                                            maxMediaSizeInMemory,
                                                                        ),
                                                                    isByMe = true,
                                                                    isPending = outboxEvent.value.eventId == null,
                                                                )
                                                        }
                                                    } else
                                                        flowOf(
                                                            EventReaction(
                                                                value = outboxEvent.key,
                                                                eventOrTransactionId =
                                                                    outboxEvent.value.eventId?.let { id ->
                                                                        EventIdOrTransactionId(id)
                                                                    }
                                                                        ?: EventIdOrTransactionId(
                                                                            outboxEvent.value.transactionId
                                                                        ),
                                                                sender =
                                                                    user.toUserInfoElement(
                                                                        this,
                                                                        matrixClient,
                                                                        initials,
                                                                        maxMediaSizeInMemory,
                                                                    ),
                                                                isByMe = true,
                                                                isPending = outboxEvent.value.eventId == null,
                                                            )
                                                        )
                                                }
                                                .toList()
                                        }
                                    }
                                }
                            }
                            .flatten()

                    combine(timelineReactions, outboxReactions, outboxRedactions) {
                        timelineEventReaction,
                        outboxEventReaction,
                        outboxRedactions ->
                        println(
                            "Combining timeline reactions: $timelineEventReaction, outbox $outboxEventReaction, redactions $outboxRedactions"
                        )
                        EventReactions(
                            outboxEventReaction.toSet() +
                                timelineEventReaction.filter { timelineMessage ->
                                    val hasNewerReactionInOutbox =
                                        timelineMessage.isByMe &&
                                            outboxEventReaction.any { it.value == timelineMessage.value }

                                    val isPendingRedaction = outboxRedactions.any {
                                        it == timelineMessage.eventOrTransactionId.eventIdOrNull()
                                    }
                                    !(hasNewerReactionInOutbox || isPendingRedaction)
                                }
                        )
                    }
                }
            }
        }
}

data class EventReaction(
    val value: String,
    val sender: UserInfoElement,
    val eventOrTransactionId: EventIdOrTransactionId,
    val isByMe: Boolean,
    val isPending: Boolean = false,
)

data class EventReactions(val all: Set<EventReaction>) {
    val byUser: Map<UserId, ByUserInfo> by lazy {
        all.groupBy { it.sender.userId }
            .mapValues { (_, value) ->
                val first = value.first()
                ByUserInfo(
                    reactions = value.associate { it.value to it.eventOrTransactionId },
                    sender = first.sender,
                    isMe = first.isByMe,
                    isPending = first.isPending,
                )
            }
    }
    val byReaction: Map<String, Set<ByReactionInfo>> by lazy {
        all.groupBy { it.value }
            .mapValues { (_, value) ->
                value
                    .map {
                        ByReactionInfo(
                            eventOrTransactionId = it.eventOrTransactionId,
                            sender = it.sender,
                            isMe = it.isByMe,
                            isPending = it.isPending,
                        )
                    }
                    .toSet()
            }
    }

    data class ByUserInfo(
        val reactions: Map<String, EventIdOrTransactionId>,
        val sender: UserInfoElement,
        val isMe: Boolean,
        val isPending: Boolean,
    )

    data class ByReactionInfo(
        val eventOrTransactionId: EventIdOrTransactionId,
        val sender: UserInfoElement,
        val isMe: Boolean,
        val isPending: Boolean,
    )

    companion object {
        val Empty = EventReactions(setOf())
    }
}
