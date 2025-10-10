package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel

interface RoomSettingsPowerlevelView {
    @Composable
    fun create(model: RoomSettingsViewModel)
}

@Composable
fun RoomSettingsPowerlevel(model: RoomSettingsViewModel) {
    DI.get<RoomSettingsPowerlevelView>().create(model)
}

class RoomSettingsPowerlevelViewImpl : RoomSettingsPowerlevelView {
    @Composable
    override fun create(model: RoomSettingsViewModel) {
        val i18n = DI.get<I18nView>()
        Row(verticalAlignment = Alignment.CenterVertically) {
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                onClick = { model.openPowerLevelView() },
            ) {
                Text(
                    text = i18n.changePowerLevelHeader(),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
