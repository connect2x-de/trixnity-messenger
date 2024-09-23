package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.EditableTextField
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsNameViewModel

interface RoomSettingsNameView {
    @Composable
    fun create(roomSettingsNameViewModel: RoomSettingsNameViewModel)
}

@Composable
fun RoomSettingsName(roomSettingsNameViewModel: RoomSettingsNameViewModel) {
    DI.get<RoomSettingsNameView>().create(roomSettingsNameViewModel)
}

class RoomSettingsNameViewImpl : RoomSettingsNameView {
    @Composable
    override fun create(roomSettingsNameViewModel: RoomSettingsNameViewModel) {
        val i18n = DI.get<I18nView>()
        EditableTextField(
            viewModel = roomSettingsNameViewModel.roomName,
            isEditable = roomSettingsNameViewModel.canChangeRoomName.collectAsState().value,
            textCaption = i18n.roomSettingsRoomName(),
            textPlaceholder = i18n.roomSettingsRoomNamePlaceholder(),
            textInfoCannotChange = i18n.roomSettingsRoomNameCannotChange(),
        )
    }
}
