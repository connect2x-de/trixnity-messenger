package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel

interface RoomSettingsExportRoomView {
    @Composable
    fun create(roomSettingsViewModel: RoomSettingsViewModel)
}

@Composable
fun RoomSettingsExportRoom(roomSettingsViewModel: RoomSettingsViewModel) {
    DI.get<RoomSettingsExportRoomView>().create(roomSettingsViewModel)
}

class RoomSettingsExportRoomViewImpl : RoomSettingsExportRoomView {
    @Composable
    override fun create(roomSettingsViewModel: RoomSettingsViewModel) {
        val i18n = DI.get<I18nView>()
        val isDirect = roomSettingsViewModel.isDirect.collectAsState().value
        val exportRoomText = i18n.exportRoom(if (isDirect) i18n.commonChat() else i18n.commonGroup())
        Row(verticalAlignment = Alignment.CenterVertically) {
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                onClick = { roomSettingsViewModel.openExportRoomView() },
            ) {
                Icon(Icons.Default.Archive, exportRoomText)
                Spacer(Modifier.size(10.dp))
                Text(
                    text = exportRoomText,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
