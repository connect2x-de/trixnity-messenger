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
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.NameEventContent


interface RoomSettingsNameViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
    ): RoomSettingsNameViewModel =
        RoomSettingsNameViewModelImpl(
            viewModelContext = viewModelContext,
            selectedRoomId = selectedRoomId,
        )

    companion object : RoomSettingsNameViewModelFactory
}

interface RoomSettingsNameViewModel {
    /** Indicates whether the current user is permitted to submit changes. */
    val canChangeRoomName: StateFlow<Boolean>

    /** Indicates whether the corresponding UI element needs to be shown. */
    val canViewRoomName: StateFlow<Boolean>

    /** Access the state and value of the room name. */
    val roomName: ApprovableTextFieldViewModel
}

class RoomSettingsNameViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsNameViewModel {
    override val canChangeRoomName: StateFlow<Boolean> =
        matrixClient.user
            .canSendEvent<NameEventContent>(selectedRoomId)
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val canViewRoomName: StateFlow<Boolean> =
        matrixClient.room
            .getById(selectedRoomId)
            .map { it?.isDirect?.not() ?: false }
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val roomName: ApprovableTextFieldViewModel =
        ApprovableTextFieldViewModelImpl(
            serverValue = matrixClient.room
                .getById(selectedRoomId)
                .map { it?.name?.explicitName ?: "" },
            coroutineScope = coroutineScope,
            onApplyChange = { newName ->
                matrixClient.api.room.sendStateEvent(
                    selectedRoomId,
                    NameEventContent(newName),
                )
            },
        )
}

class PreviewRoomSettingsNameViewModel : RoomSettingsNameViewModel {
    override val roomName: ApprovableTextFieldViewModel = PreviewApprovableTextFieldViewModel()
    override val canChangeRoomName: StateFlow<Boolean> = MutableStateFlow(true)
    override val canViewRoomName: StateFlow<Boolean> = MutableStateFlow(true)
}
