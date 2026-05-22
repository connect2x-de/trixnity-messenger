package de.connect2x.trixnity.messenger.compose.view.previews

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomSettingsContainer
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PreviewRoomSettingsViewModel

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun RoomSettingsPreview() {
    InitMessengerPreview {
        Row(Modifier.sizeIn(maxWidth = 500.dp)) {
            val settingsModel = PreviewRoomSettingsViewModel()
            RoomSettingsContainer(roomSettingsViewModel = settingsModel, isSinglePane = false)
        }
    }
}
