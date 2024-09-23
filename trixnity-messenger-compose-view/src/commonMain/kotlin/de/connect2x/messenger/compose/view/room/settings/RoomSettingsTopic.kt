package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.EditableTextField
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsTopicViewModel

interface RoomSettingsTopicView {
    @Composable
    fun create(roomSettingsTopicViewModel: RoomSettingsTopicViewModel)
}

@Composable
fun RoomSettingsTopic(roomSettingsTopicViewModel: RoomSettingsTopicViewModel) {
    DI.get<RoomSettingsTopicView>().create(roomSettingsTopicViewModel)
}

class RoomSettingsTopicViewImpl : RoomSettingsTopicView {
    @Composable
    override fun create(roomSettingsTopicViewModel: RoomSettingsTopicViewModel) {
        val i18n = DI.get<I18nView>()
        EditableTextField(
            viewModel = roomSettingsTopicViewModel.roomTopic,
            isEditable = roomSettingsTopicViewModel.canChangeRoomTopic.collectAsState().value,
            textCaption = i18n.roomSettingsRoomTopic(),
            textPlaceholder = i18n.roomSettingsRoomTopicPlaceholder(),
            textInfoCannotChange = i18n.roomSettingsRoomTopicCannotChange(),
        )
    }
}
