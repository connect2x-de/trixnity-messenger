package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent

interface LocationMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEvent: TimelineEvent?,
        content: RoomMessageEventContent.Location,
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        showBigGap: Boolean,
        showSender: Flow<Boolean>,
        sender: Flow<UserInfoElement>,
        invitation: Flow<String?>,
    ): LocationMessageViewModel {
        return LocationMessageViewModelImpl(
            viewModelContext,
            timelineEvent,
            content,
            formattedDate,
            showDateAbove,
            formattedTime,
            isByMe,
            showChatBubbleEdge,
            showBigGap,
            showSender,
            sender,
            invitation,
        )
    }

    companion object : LocationMessageViewModelFactory
}

interface LocationMessageViewModel : RoomMessageViewModel {
    val geoUri: String
    val name: String
}

class LocationMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEvent: TimelineEvent?,
    content: RoomMessageEventContent.Location,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    override val formattedTime: String?,
    override val isByMe: Boolean,
    override val showChatBubbleEdge: Boolean,
    override val showBigGap: Boolean,
    showSender: Flow<Boolean>,
    sender: Flow<UserInfoElement>,
    invitation: Flow<String?>,
) : LocationMessageViewModel, MatrixClientViewModelContext by viewModelContext {
    override val showReactions: StateFlow<Boolean> = MutableStateFlow(true)
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<UserInfoElement> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), UserInfoElement("", UserId("")))
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    override val geoUri = content.geoUri
    override val name = content.body
}
