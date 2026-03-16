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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
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
    val changeMessage: StateFlow<String?>
    val undecryptableHistoryInfo: StateFlow<String?>
}

private enum class UserInfoChangeEvent {
    CHANGE_AVATAR,
    REMOVE_DISPLAY_NAME,
    CHANGE_DISPLAY_NAME
}

private sealed class ChangeEvent(open val event: StateEvent<*>) {
    data class Membership(val kind: MembershipChange, override val event: StateEvent<*>) : ChangeEvent(event)
    data class UserInfo(val kind: UserInfoChangeEvent, override val event: StateEvent<*>, val previousContent: MemberEventContent) : ChangeEvent(event)
    data class NoPreviousContent(val kind: MembershipChange, override val event: StateEvent<*>) : ChangeEvent(event)
}

class MemberStateTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: MemberEventContent,
    roomId: RoomId,
    eventId: EventId,
) : MemberStateTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val changeEvent : Flow<ChangeEvent?> =
        matrixClient.room.getTimelineEvent(roomId, eventId).filterNotNull().map { timelineEvent ->
            val event = timelineEvent.event
            require(event is StateEvent)

            val previousContent = event.unsigned?.previousContent
            if (previousContent is MemberEventContent) {
                val membershipEvent =
                    ChangeEvent.Membership(
                        membershipChangedEvent(event, content, previousContent),
                        event
                    )
                when {
                    content.membership != previousContent.membership ->
                        membershipEvent

                    content.avatarUrl != previousContent.avatarUrl ->
                        ChangeEvent.UserInfo(UserInfoChangeEvent.CHANGE_AVATAR, event, previousContent)

                    content.displayName != previousContent.displayName -> {
                        if (content.displayName == null)
                            ChangeEvent.UserInfo(
                                UserInfoChangeEvent.REMOVE_DISPLAY_NAME,
                                event,
                                previousContent
                            )
                        else
                            ChangeEvent.UserInfo(
                                UserInfoChangeEvent.CHANGE_DISPLAY_NAME,
                                event,
                                previousContent
                            )
                    }

                    else -> {
                        // This event is not very precise because it is also triggered when there is no change at all.
                        // Emitting null would lead to the UI waiting for a vaue.
                        membershipEvent
                    }
                }
            } else {
                ChangeEvent.NoPreviousContent(membershipChangedEvent(event, content), event)
            }
        }

    
    @OptIn(ExperimentalCoroutinesApi::class)
    override val changeMessage = 
        changeEvent.filterNotNull()
            .flatMapConcat { changeEvent ->
                matrixClient.user.getById(roomId, changeEvent.event.sender).map { sender ->
                    Pair(changeEvent, sender)
                }
            }
            .flatMapLatest { (changeEvent, sender) -> 
                val event = changeEvent.event
                val senderName = sender?.name ?: event.sender.full
                when (changeEvent) {
                    is ChangeEvent.UserInfo ->
                        when (changeEvent.kind) {
                            UserInfoChangeEvent.CHANGE_AVATAR -> flowOf(i18n.eventChangeAvatar(senderName))
                            UserInfoChangeEvent.REMOVE_DISPLAY_NAME ->
                                flowOf(i18n.eventRemoveDisplayName(changeEvent.previousContent.displayName ?: event.sender.full))
                            UserInfoChangeEvent.CHANGE_DISPLAY_NAME ->
                                flowOf(i18n.eventChangeDisplayName(
                                    changeEvent.previousContent.displayName ?: event.sender.full,
                                    content.displayName))
                        }
                    is ChangeEvent.Membership ->
                        membershipChangedText(event, content, senderName, changeEvent.kind)
                    is ChangeEvent.NoPreviousContent ->
                        membershipChangedText(event, content, senderName, changeEvent.kind)
                }
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)
    
    @OptIn(ExperimentalCoroutinesApi::class)
    override val undecryptableHistoryInfo =
        combine(
            changeEvent.filterNotNull(),
            matrixClient.room.getById(roomId).filterNotNull(),
            matrixClient.room.getState<HistoryVisibilityEventContent>(roomId)
                .mapNotNull { it?.content?.historyVisibility }
        ) { 
            changeEvent, room, historyVisibility -> 
                Triple(changeEvent, room, historyVisibility)
        }
                .flatMapLatest { (changeEvent, room, historyVisibility) ->
                    val affectedUser = UserId(changeEvent.event.stateKey)
                    val currentUserAffected = matrixClient.userId == affectedUser
                    if (room.encrypted && currentUserAffected) {
                        when (historyVisibility) {
                            HistoryVisibilityEventContent.HistoryVisibility.INVITED,
                            HistoryVisibilityEventContent.HistoryVisibility.SHARED,
                            HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE-> {
                                val invited =
                                    changeEvent is ChangeEvent.NoPreviousContent && changeEvent.kind == MembershipChange.INVITE
                                if (invited) {
                                    flowOf(
                                        i18n.eventChangeUndecryptableHistoryInfo()
                                    )
                                } else {
                                    flowOf()
                                }

                            }

                            HistoryVisibilityEventContent.HistoryVisibility.JOINED -> {
                                val joined =
                                    changeEvent is ChangeEvent.Membership && changeEvent.kind == MembershipChange.JOIN
                                if (joined) {
                                    flowOf(
                                        i18n.eventChangeUndecryptableHistoryInfo()
                                    )
                                } else {
                                    flowOf()
                                }
                            }
                        }
                    }
                    else {
                        flowOf()
                    }
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)
    
    private fun membershipChangedEvent(
        event: StateEvent<*>,
        content: MemberEventContent,
        previousContent: MemberEventContent? = null,
    ): MembershipChange {
        val affectedUser = UserId(event.stateKey)
        return MembershipChange.of(
            from = previousContent?.membership,
            to = content.membership,
            appliedToSelf = affectedUser == event.sender,
        )
    }
        
    private fun membershipChangedText(
        event: StateEvent<*>,
        content: MemberEventContent,
        senderName: String,
        stateTransition: MembershipChange
    ): Flow<String> {
        val affectedUser = UserId(event.stateKey)
        val affectedUsernameFlow = 
            matrixClient.user.getById(event.roomId, affectedUser)
                .map { user -> user?.name ?: affectedUser.full }
        val isDirectFlow = matrixClient.room.getById(event.roomId).filterNotNull().map { it.isDirect }
        return combine(affectedUsernameFlow, isDirectFlow) { affectedUsername, isDirect ->
            val groupOrChatDative =
                if (isDirect) i18n.eventChangeChatDative()
                else i18n.eventChangeGroupDative()
            val groupOrChatAccusative =
                if (isDirect) i18n.eventChangeChatAccusative()
                else i18n.eventChangeGroupAccusative()
            
            when (stateTransition) {
                MembershipChange.INVITE -> 
                    i18n.eventChangeInvite(affectedUsername, senderName, content.reason)
                MembershipChange.JOIN -> 
                    i18n.eventChangeJoin(affectedUsername, groupOrChatDative)
                MembershipChange.BAN -> i18n.eventChangeBan(
                    affectedUsername,
                    senderName,
                    groupOrChatDative,
                    content.reason
                )
                MembershipChange.KNOCK -> 
                    i18n.eventChangeKnock(affectedUsername, groupOrChatDative, content.reason)
                MembershipChange.UNBAN -> 
                    i18n.eventChangeUnban(affectedUsername, senderName, content.reason)
                MembershipChange.INVITE_REJECT -> 
                    i18n.eventChangeRejected(affectedUsername, content.reason)
                MembershipChange.INVITE_REVOKE -> 
                    i18n.eventChangeRevoked(affectedUsername, senderName, content.reason)
                MembershipChange.LEAVE -> 
                    i18n.eventChangeLeave(affectedUsername, groupOrChatAccusative)
                MembershipChange.KICK -> 
                    i18n.eventChangeKick(
                    affectedUsername,
                    senderName,
                    groupOrChatDative,
                    content.reason
                )
            }
        }
    }
}

