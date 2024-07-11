package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent

interface RoomEncryptionEnableViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        formattedDate: String,
        timelineEvent: TimelineEvent?,
        content: EncryptionEventContent,
        showDateAbove: Boolean,
        invitation: Flow<String?>,
        sender: Flow<UserInfoElement>,
        isDirectFlow: StateFlow<Boolean>
    ): RoomEncryptionEnableViewModel =
        RoomEncryptionEnableViewModelImpl(
            viewModelContext,
            timelineEvent,
            content,
            formattedDate,
            showDateAbove,
            invitation,
            sender,
            isDirectFlow
        )

    companion object : RoomEncryptionEnableViewModelFactory
}

interface RoomEncryptionEnableViewModel: BaseTimelineElementViewModel {
    val roomEncryptionEnableMessage: StateFlow<String?>
}

class RoomEncryptionEnableViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEvent: TimelineEvent?,
    content: EncryptionEventContent,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    invitation: Flow<String?>,
    sender: Flow<UserInfoElement>,
    isDirectFlow: StateFlow<Boolean>
): MatrixClientViewModelContext by viewModelContext, RoomEncryptionEnableViewModel {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    @OptIn(ExperimentalCoroutinesApi::class)
    override val roomEncryptionEnableMessage: StateFlow<String?> = sender.mapLatest { userInfo ->
        return@mapLatest i18n.roomEncryptionEnabled(userInfo.name)
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
}
