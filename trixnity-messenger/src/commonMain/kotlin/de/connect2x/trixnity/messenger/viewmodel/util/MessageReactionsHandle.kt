package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.MessageUserReactions.ReactionEvent
import de.connect2x.trixnity.messenger.viewmodel.util.MessageUserReactions.UserReactions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.flatten
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
) : MessageReactionsHandle {
    override val reactions =
        client
            .getMessageUserReactions(
                roomId = roomId,
                eventId = eventId,
                initials = initials,
                config = config,
            )
}

data class MessageUserReactions(
    val byUser: Map<UserId, UserReactions>,
    val byReaction: Map<ReactionKey, Set<ReactionEvent>>,
) {
    data class UserReactions(
        val sender: UserInfoElement?,
        val reactions: Set<ReactionKey>,
    )

    data class ReactionEvent(
        val eventId: EventId,
        val sender: UserInfoElement,
        val isMe: Boolean,
    )

    companion object {
        val Empty = MessageUserReactions(mapOf(), mapOf())
    }
}

// TODO: should consider outbox to get immediate feedback
fun MatrixClient.getMessageUserReactions(
    roomId: RoomId,
    eventId: EventId,
    initials: Initials,
    config: MatrixMessengerConfiguration,
): Flow<MessageUserReactions> =
    room
        .getTimelineEventReactionAggregation(roomId, eventId)
        .debounceAfterFirst(500.milliseconds)
        .map { reactions ->
            reactions.reactions.mapValues {
                combine(it.value.map { event ->
                    user
                        .getById(event.roomId, event.sender)
                        .map { user -> Pair(user, event) }
                }) { it }
            }
        }
        .flatten()
        .scopedMapLatest { reactions ->
            if (reactions.isEmpty()) {
                return@scopedMapLatest MessageUserReactions.Empty
            }
            val byUser = mutableMapOf<UserId, Pair<UserInfoElement, MutableSet<ReactionKey>>>()
            val byReaction: Map<ReactionKey, Set<ReactionEvent>> = reactions
                .mapValues { (reactionKey, events) ->
                    events?.mapNotNull { (roomUser, timelineEvent) ->
                        if (roomUser == null) {
                            return@mapNotNull null
                        }
                        val userId = roomUser.userId
                        val userInfo = roomUser.toUserInfoElement(
                            coroutineScope = this@scopedMapLatest,
                            matrixClient = this@getMessageUserReactions,
                            initials = initials,
                            maxAvatarSize = config.avatarMaxSize,
                            fallbackUserId = userId,
                        )

                        byUser.getOrPut(userId) { Pair(userInfo, mutableSetOf()) }
                            .second.add(reactionKey)

                        ReactionEvent(
                            eventId = timelineEvent.eventId,
                            sender = userInfo,
                            isMe = this@getMessageUserReactions.userId == roomUser.userId,
                        )
                    }?.toSet() ?: setOf()
                }
            MessageUserReactions(
                byUser = byUser.mapValues { (_, info) ->
                    UserReactions(
                        sender = info.first,
                        reactions = info.second.toSet(),
                    )
                },
                byReaction = byReaction,
            )
        }

typealias ReactionKey = String
