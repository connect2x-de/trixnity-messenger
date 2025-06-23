package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.PreviewApprovableTextFieldViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.TopicEventContent


interface RoomSettingsTopicViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
    ): RoomSettingsTopicViewModel =
        RoomSettingsTopicViewModelImpl(
            viewModelContext = viewModelContext,
            selectedRoomId = selectedRoomId,
        )

    companion object : RoomSettingsTopicViewModelFactory
}

interface RoomSettingsTopicViewModel {
    /** Indicates whether the current user is permitted to submit changes. */
    val canChangeRoomTopic: StateFlow<Boolean>

    /** Indicates whether the corresponding UI element needs to be shown. */
    val canViewRoomTopic: StateFlow<Boolean>

    /** Access the state and value of the room topic. */
    val roomTopic: ApprovableTextFieldViewModel
}

class RoomSettingsTopicViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsTopicViewModel {
    override val canChangeRoomTopic: StateFlow<Boolean> =
        matrixClient.user
            .canSendEvent<TopicEventContent>(selectedRoomId)
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val canViewRoomTopic: StateFlow<Boolean> =
        matrixClient.room
            .getById(selectedRoomId)
            .map { it?.isDirect?.not() ?: false }
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val roomTopic: ApprovableTextFieldViewModel =
        ApprovableTextFieldViewModelImpl(
            serverValue = matrixClient.room
                .getState<TopicEventContent>(roomId = selectedRoomId)
                .map { it?.content?.topic ?: "" },
            maxLength = 20_000,
            coroutineScope = coroutineScope,
            onApplyChange = { newTopic ->
                matrixClient.api.room.sendStateEvent(
                    selectedRoomId,
                    TopicEventContent(newTopic),
                )
            },
        )
}

class PreviewRoomSettingsTopicViewModel : RoomSettingsTopicViewModel {
    override val roomTopic: ApprovableTextFieldViewModel = PreviewApprovableTextFieldViewModel()
    override val canChangeRoomTopic: StateFlow<Boolean> = MutableStateFlow(true)
    override val canViewRoomTopic: StateFlow<Boolean> = MutableStateFlow(true)
}
