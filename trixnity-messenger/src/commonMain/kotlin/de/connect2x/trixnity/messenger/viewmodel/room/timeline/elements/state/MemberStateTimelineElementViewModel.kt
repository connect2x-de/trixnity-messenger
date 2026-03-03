package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.State
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.util.MembershipChange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.store.sender
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import kotlin.reflect.KClass

interface MemberStateTimelineElementViewModelFactory : TimelineElementViewModelFactory<MemberEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: MemberEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): MemberStateTimelineElementViewModel? =
        if (eventIdOrTransactionId is EventIdOrTransactionId.EventId)
            MemberStateTimelineElementViewModelImpl(
                viewModelContext,
                content,
                roomId,
                eventIdOrTransactionId.eventId,
            ) else null

    override val supports: KClass<MemberEventContent>
        get() = MemberEventContent::class

    companion object : MemberStateTimelineElementViewModelFactory
}

interface MemberStateTimelineElementViewModel : State<MemberEventContent> {
    val changeMessage: StateFlow<ChangeMessage?>
}

sealed interface ChangeMessage {
    data class Simple(val message: String) : ChangeMessage
    data class WithDivider(val main: String, val divider: String) : ChangeMessage
}

class MemberStateTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: MemberEventContent,
    roomId: RoomId,
    eventId: EventId,
) : MemberStateTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val changeMessage =
        flow {
            val timelineEventSnapshot = matrixClient.room.getTimelineEvent(roomId, eventId).filterNotNull().first()
            emitAll(
                combine(
                    matrixClient.user.getById(roomId, timelineEventSnapshot.sender),
                    matrixClient.room.getById(roomId).filterNotNull().map { it.isDirect },
                    matrixClient.room.getTimelineEvent(roomId, eventId).filterNotNull(),
                ) { userInfo, isDirect, timelineEvent ->
                    Triple(userInfo, isDirect, timelineEvent)
                }.flatMapLatest { (userInfo, isDirect, timelineEvent) ->
                    val event = timelineEvent.event
                    require(event is StateEvent)

                    val name = userInfo?.name ?: timelineEventSnapshot.sender.full
                    val previousContent = event.unsigned?.previousContent
                    if (previousContent is MemberEventContent) {
                        when {
                            content.membership != previousContent.membership -> {
                                membershipChanged(event, content, name, isDirect, previousContent)
                            }
                            content.avatarUrl != previousContent.avatarUrl -> {
                                flowOf(ChangeMessage.Simple(i18n.eventChangeAvatar(name)))
                            }
                            content.displayName != previousContent.displayName -> {
                                if (content.displayName == null) {
                                    flowOf(ChangeMessage.Simple(i18n.eventRemoveDisplayName(previousContent.displayName ?: event.sender.full)))
                                } else {
                                    flowOf(
                                        ChangeMessage.Simple(
                                            i18n.eventChangeDisplayName(
                                                previousContent.displayName ?: event.sender.full,
                                                content.displayName
                                            )
                                        )
                                    )
                                }
                            }
                            else -> {
                                // This message is not very precise because it is also triggered when there is no change at all.
                                // Emitting null would lead to the UI waiting for a value.
                                membershipChanged(event, content, name, isDirect, previousContent)
                            }
                        }
                    } else {
                        membershipChanged(event, content, name, isDirect)
                    }
                }
            )
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    private fun membershipChanged(
        event: StateEvent<*>,
        content: MemberEventContent,
        username: String,
        isDirect: Boolean,
        previousContent: MemberEventContent? = null,
    ): Flow<ChangeMessage> {
        val groupOrChatDative =
            if (isDirect) i18n.eventChangeChatDative()
            else i18n.eventChangeGroupDative()
        val groupOrChatAccusative =
            if (isDirect) i18n.eventChangeChatAccusative()
            else i18n.eventChangeGroupAccusative()

        val affectedUser = UserId(event.stateKey)
        val stateTransition = MembershipChange.of(
            from = previousContent?.membership,
            to = content.membership,
            appliedToSelf = affectedUser == event.sender,
        )
        val usernameFlow = 
            matrixClient.user.getById(event.roomId, affectedUser)
                .map { user -> user?.name ?: affectedUser.full }
        val roomFlow = matrixClient.room.getById(event.roomId).filterNotNull()
        return combine(usernameFlow, roomFlow) { thisUsername, room ->
            when (stateTransition) {
                MembershipChange.INVITE -> 
                    ChangeMessage.Simple(i18n.eventChangeInvite(thisUsername, username, content.reason))
                MembershipChange.JOIN -> {
                        val currentUserJoined = matrixClient.userId == affectedUser
                        val preJoinHistoryNotAvailable = room.encrypted && currentUserJoined 
                        if (preJoinHistoryNotAvailable) {
                            ChangeMessage.WithDivider(
                                i18n.eventChangeJoin(thisUsername, groupOrChatDative),
                                i18n.eventChangePreJoinHistoryNotAvailable()
                            )
                        } else {
                            ChangeMessage.Simple(
                                i18n.eventChangeJoin(thisUsername, groupOrChatDative),
                            )
                        }
                    }
                MembershipChange.BAN -> ChangeMessage.Simple(i18n.eventChangeBan(
                    thisUsername,
                    username,
                    groupOrChatDative,
                    content.reason
                ))
                MembershipChange.KNOCK -> 
                    ChangeMessage.Simple(i18n.eventChangeKnock(thisUsername, groupOrChatDative, content.reason))
                MembershipChange.UNBAN -> 
                    ChangeMessage.Simple(i18n.eventChangeUnban(thisUsername, username, content.reason))
                MembershipChange.INVITE_REJECT -> 
                    ChangeMessage.Simple(i18n.eventChangeRejected(thisUsername, content.reason))
                MembershipChange.INVITE_REVOKE -> 
                    ChangeMessage.Simple(i18n.eventChangeRevoked(thisUsername, username, content.reason))
                MembershipChange.LEAVE -> 
                    ChangeMessage.Simple(i18n.eventChangeLeave(thisUsername, groupOrChatAccusative))
                MembershipChange.KICK -> 
                    ChangeMessage.Simple(i18n.eventChangeKick(
                    thisUsername,
                    username,
                    groupOrChatDative,
                    content.reason
                ))
            }
        }
    }
}

