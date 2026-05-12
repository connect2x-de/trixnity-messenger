package de.connect2x.trixnity.messenger.compose.view.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.trixnity.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomSettingsAlias
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PreviewRoomSettingsAliasViewModel

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun RoomSettingsAliasPreview() {
    InitMessengerPreview {
        val viewModel = PreviewRoomSettingsAliasViewModel()
        viewModel.isUpdating.value = true
        RoomSettingsAlias(viewModel)
    }
}
