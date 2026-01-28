package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.PreviewApprovableTextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.client.user.canSendEvent
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.NameEventContent
import org.koin.core.component.get


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

    private val defaultRoomName = get<RoomName>().getRoomName(selectedRoomId, matrixClient)
        .stateIn(coroutineScope, WhileSubscribed(), "")

    override val roomName: ApprovableTextFieldViewModel =
        ApprovableTextFieldViewModelImpl(
            serverValue = defaultRoomName,
            maxLength = 1_000,
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
