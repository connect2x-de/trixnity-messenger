package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership

interface MemberStatusViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEventFlow: Flow<TimelineEvent?>,
        content: MemberEventContent,
        formattedDate: String,
        showDateAbove: Boolean,
        invitation: Flow<String?>,
        sender: Flow<UserInfoElement>,
        isDirectFlow: StateFlow<Boolean>,
    ): MemberStatusViewModel {
        return MemberStatusViewModelImpl(
            viewModelContext, timelineEventFlow, content, formattedDate, showDateAbove, invitation, sender, isDirectFlow
        )
    }

    companion object : MemberStatusViewModelFactory
}

interface MemberStatusViewModel : BaseTimelineElementViewModel {
    val formattedMemberStatus: StateFlow<String?>
}

open class MemberStatusViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEventFlow: Flow<TimelineEvent?>,
    content: MemberEventContent,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    invitation: Flow<String?>,
    sender: Flow<UserInfoElement>,
    isDirectFlow: StateFlow<Boolean>,
) : MemberStatusViewModel, MatrixClientViewModelContext by viewModelContext {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val formattedMemberStatus: StateFlow<String?> = combine(
        timelineEventFlow,
        sender,
        isDirectFlow,
    ) { timelineEvent, userInfo, isDirect ->
        timelineEvent?.let {
            val event = it.event
            require(event is StateEvent)
            val content = event.content
            require(content is MemberEventContent)

            val previousContent = event.unsigned?.previousContent
            if (previousContent is MemberEventContent) {
                if (content.membership != previousContent.membership) {
                    membershipChanged(event, content, userInfo.name, isDirect)
                } else if (content.avatarUrl != previousContent.avatarUrl) {
                    i18n.eventChangeAvatar(userInfo.name)
                } else {
                    null
                }
            } else {
                membershipChanged(event, content, userInfo.name, isDirect)
            }
        }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private suspend fun membershipChanged(
        event: StateEvent<*>,
        content: MemberEventContent,
        username: String,
        isDirect: Boolean
    ): String {
        val groupOrChatDative =
            if (isDirect) i18n.eventChangeChatDative()
            else i18n.eventChangeGroupDative()
        val groupOrChatAccusative =
            if (isDirect) i18n.eventChangeChatAccusative()
            else i18n.eventChangeGroupAccusative()
        val thisUserId = UserId(event.stateKey)
        val thisUsername = matrixClient.user.getById(event.roomId, thisUserId)
            .map { user -> user?.name ?: thisUserId.full }
            .stateIn(coroutineScope)

        return when (content.membership) {
            Membership.INVITE -> i18n.eventChangeInvite(thisUsername.value, username)
            Membership.JOIN -> i18n.eventChangeJoin(thisUsername.value, groupOrChatDative)
            Membership.LEAVE -> i18n.eventChangeLeave(thisUsername.value, groupOrChatAccusative)
            Membership.BAN -> i18n.eventChangeBan(thisUsername.value, username, groupOrChatDative)
            Membership.KNOCK -> i18n.eventChangeKnock(thisUsername.value, groupOrChatDative)
            else -> ""
        }
    }
}