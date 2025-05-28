package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.State
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
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
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
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
                        if (content.membership != previousContent.membership) {
                            membershipChanged(event, content, name, isDirect, previousContent)
                        } else if (content.avatarUrl != previousContent.avatarUrl) {
                            flowOf(i18n.eventChangeAvatar(name))
                        } else if (content.displayName != previousContent.displayName) {
                            flowOf(i18n.eventChangeDisplayName(previousContent.displayName, content.displayName))
                        } else {
                            // This message is not very precise because it is also triggered when there is no change at all.
                            // Emitting null would lead to the UI waiting for a value.
                            membershipChanged(event, content, name, isDirect, previousContent)
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
    ): Flow<String> {
        val senderIsStateKey = event.stateKey == event.sender.full
        //Sender = stateKey -> Invitation was rejected, else it was revoked
        val rejectedInvitation =
            previousContent?.membership == Membership.INVITE && content.membership == Membership.LEAVE && senderIsStateKey
        val revokedInvitation =
            previousContent?.membership == Membership.INVITE && content.membership == Membership.LEAVE && !senderIsStateKey
        val groupOrChatDative =
            if (isDirect) i18n.eventChangeChatDative()
            else i18n.eventChangeGroupDative()
        val groupOrChatAccusative =
            if (isDirect) i18n.eventChangeChatAccusative()
            else i18n.eventChangeGroupAccusative()
        val thisUserId = UserId(event.stateKey)
        return matrixClient.user.getById(event.roomId, thisUserId)
            .map { user -> user?.name ?: thisUserId.full }
            .map { thisUsername ->
                if (rejectedInvitation) {
                    i18n.eventChangeRejected(thisUsername, content.reason)
                } else if (revokedInvitation) {
                    i18n.eventChangeRevoked(thisUsername, username, content.reason)
                } else {
                    when (content.membership) {
                        Membership.INVITE -> i18n.eventChangeInvite(thisUsername, username, content.reason)
                        Membership.JOIN -> i18n.eventChangeJoin(thisUsername, groupOrChatDative)
                        Membership.LEAVE ->
                            if (senderIsStateKey) i18n.eventChangeLeave(thisUsername, groupOrChatAccusative)
                            else i18n.eventChangeKick(thisUsername, username, groupOrChatDative, content.reason)
                        Membership.BAN -> i18n.eventChangeBan(thisUsername, username, groupOrChatDative, content.reason)
                        Membership.KNOCK -> i18n.eventChangeKnock(thisUsername, groupOrChatDative, content.reason)
                    }
                }
            }
    }
}
