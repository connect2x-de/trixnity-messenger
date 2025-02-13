package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.util.ReactionKey
import de.connect2x.trixnity.messenger.viewmodel.util.MessageUserReactionsOld.ReactionCount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getTimelineEventReactionAggregation
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId


// TODO: should consider outbox to get immediate feedback
@OptIn(ExperimentalCoroutinesApi::class)
fun getMessageUserReactions(
    client: MatrixClient,
    roomId: RoomId,
    eventId: EventId,
): Flow<MessageUserReactionsOld> =
    client.room.getTimelineEventReactionAggregation(roomId, eventId)
        .mapLatest { reactions ->
            val reactionsAggregation = reactions.reactions
            if (reactionsAggregation.isEmpty()) {
                return@mapLatest MessageUserReactionsOld()
            }
            val byUser: MutableMap<UserId, MutableSet<ReactionKey>> = mutableMapOf()
            val byCount: Map<ReactionKey, ReactionCount> = reactionsAggregation
                .mapValues { (key, events) ->
                    events.forEach {
                        byUser.getOrPut(it.sender) { mutableSetOf() }.add(key)
                    }
                    ReactionCount(events)
                }
            MessageUserReactionsOld(
                byUser = byUser.mapValues { (userId, reactionKeys) ->
                    MessageUserReactionsOld.UserReactions(
                        client.user.getById(roomId, userId),
                        reactions = reactionKeys.toSet(),
                    )
                },
                byCount = byCount,
            )
        }

data class MessageUserReactionsOld(
    val byUser: Map<UserId, UserReactions> = emptyMap(),
    val byCount: Map<ReactionKey, ReactionCount> = emptyMap(),
) {
    data class ReactionCount(val events: Set<TimelineEvent>) {
        val count: UInt get() = events.size.toUInt()
    }

    data class UserReactions(
        val roomUserFlow: Flow<RoomUser?>,
        val reactions: Set<ReactionKey>,
    )
}
