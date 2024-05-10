package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent

interface HistoryVisibilityChangeStatusViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEvent: TimelineEvent?,
        content: HistoryVisibilityEventContent,
        formattedDate: String,
        showDateAbove: Boolean,
        invitation: Flow<String?>,
        sender: Flow<UserInfoElement>,
        isDirectFlow: StateFlow<Boolean>,
    ): HistoryVisibilityChangeStatusViewModel {
        return HistoryVisibilityChangeStatusViewModelImpl(
            viewModelContext,
            timelineEvent,
            content,
            formattedDate,
            showDateAbove,
            invitation,
            sender,
            isDirectFlow,
        )
    }

    companion object : HistoryVisibilityChangeStatusViewModelFactory
}

interface HistoryVisibilityChangeStatusViewModel : BaseTimelineElementViewModel {
    val historyVisibilityMessage: StateFlow<String?>
}

class HistoryVisibilityChangeStatusViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEvent: TimelineEvent?,
    content: HistoryVisibilityEventContent,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    invitation: Flow<String?>,
    sender: Flow<UserInfoElement>,
    isDirectFlow: StateFlow<Boolean>,
) : MatrixClientViewModelContext by viewModelContext, HistoryVisibilityChangeStatusViewModel {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val historyVisibilityMessage =
        combine(sender, isDirectFlow) { userInfo, isDirect ->
            val unsigned = timelineEvent?.event?.unsigned
            val previousContent =
                if (unsigned is UnsignedRoomEventData.UnsignedStateEventData) unsigned.previousContent else null
            val from = if (previousContent is HistoryVisibilityEventContent) {
                i18n.eventChangeFrom(translateVisibility(previousContent.historyVisibility))
            } else ""

            val groupOrChat =
                if (isDirect) i18n.eventChangeChatGenitive()
                else i18n.eventChangeGroupGenitive()

            i18n.historyVisibilityChange(userInfo.name, groupOrChat, from, translateVisibility(content.historyVisibility))
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    fun translateVisibility(historyVisibility: HistoryVisibilityEventContent.HistoryVisibility):String {
       return when (historyVisibility) {
            HistoryVisibilityEventContent.HistoryVisibility.SHARED -> i18n.historyVisibilityShared()
           HistoryVisibilityEventContent.HistoryVisibility.JOINED -> i18n.historyVisibilityJoined()
           HistoryVisibilityEventContent.HistoryVisibility.INVITED -> i18n.historyVisibiltyInvite()
           HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE -> i18n.historyVisibilityWorldReadable()
        }
    }

}
