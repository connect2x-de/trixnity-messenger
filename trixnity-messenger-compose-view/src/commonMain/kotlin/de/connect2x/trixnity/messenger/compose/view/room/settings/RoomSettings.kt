package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel

@Composable
fun RoomSettingsContainer(roomSettingsViewModel: RoomSettingsViewModel, isSinglePane: Boolean) {
    Box(Modifier.fillMaxWidth().clickable(enabled = false) {}) {
        Box(Modifier.fillMaxHeight().align(Alignment.CenterEnd)) { RoomSettings(roomSettingsViewModel, isSinglePane) }
    }
}

interface RoomSettingsView {
    @Composable fun create(roomSettingsViewModel: RoomSettingsViewModel, isSinglePane: Boolean)
}

@Composable
fun RoomSettings(roomSettingsViewModel: RoomSettingsViewModel, isSinglePane: Boolean) {
    DI.get<RoomSettingsView>().create(roomSettingsViewModel, isSinglePane)
}

class RoomSettingsViewImpl : RoomSettingsView {
    @Composable
    override fun create(roomSettingsViewModel: RoomSettingsViewModel, isSinglePane: Boolean) {
        val i18n = DI.get<I18nView>()
        SettingsPane(roomSettingsViewModel, isSinglePane, i18n.roomSettings()) {
            SettingsIdentity(
                roomSettingsViewModel,
                name = { RoomSettingsName(roomSettingsViewModel.roomSettingsNameViewModel) },
                topic = { RoomSettingsTopic(roomSettingsViewModel.roomSettingsTopicViewModel) },
            )
            HorizontalDivider()
            RoomSettingsSecurity(roomSettingsViewModel.roomSettingsSecurityViewModel)
            HorizontalDivider()
            RoomSettingsNotifications(roomSettingsViewModel.roomSettingsNotificationsViewModel)
            HorizontalDivider()
            RoomSettingsHistoryVisibility(roomSettingsViewModel)
            HorizontalDivider()
            RoomSettingsJoinRules(roomSettingsViewModel)
            HorizontalDivider()
            RoomSettingsMemberList(roomSettingsViewModel)
            HorizontalDivider()
            RoomSettingsPowerlevel(roomSettingsViewModel)
            RoomSettingsExportRoom(roomSettingsViewModel)
            SettingsLeave(roomSettingsViewModel)
        }
    }
}
