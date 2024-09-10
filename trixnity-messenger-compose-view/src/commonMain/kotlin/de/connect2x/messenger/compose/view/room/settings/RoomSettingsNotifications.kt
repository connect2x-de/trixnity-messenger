package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.NotificationLevel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.NotificationLevels
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsNotificationsViewModel

interface RoomSettingsNotificationsView {
    @Composable
    fun create(roomSettingsNotificationsViewModel: RoomSettingsNotificationsViewModel)
}

@Composable
fun RoomSettingsNotifications(roomSettingsNotificationsViewModel: RoomSettingsNotificationsViewModel) {
    DI.current.get<RoomSettingsNotificationsView>().create(roomSettingsNotificationsViewModel)
}

class RoomSettingsNotificationsViewImpl : RoomSettingsNotificationsView {
    @Composable
    override fun create(roomSettingsNotificationsViewModel: RoomSettingsNotificationsViewModel) {
        val i18n = DI.current.get<I18nView>()
        val isLoading = roomSettingsNotificationsViewModel.isNotificationsLevelLoading.collectAsState().value

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = i18n.commonNotifications().capitalize(Locale.current),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (isLoading) {
                    Spacer(Modifier.size(20.dp))
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
            RoomSettingsNotificationsLevel(
                roomSettingsNotificationsViewModel,
                roomSettingsNotificationsViewModel.roomNotificationLevels.getValue(NotificationLevels.DEFAULT)
            )
            RoomSettingsNotificationsLevel(
                roomSettingsNotificationsViewModel,
                roomSettingsNotificationsViewModel.roomNotificationLevels.getValue(NotificationLevels.ALL)
            )
            RoomSettingsNotificationsLevel(
                roomSettingsNotificationsViewModel,
                roomSettingsNotificationsViewModel.roomNotificationLevels.getValue(NotificationLevels.MENTIONS)
            )
            RoomSettingsNotificationsLevel(
                roomSettingsNotificationsViewModel,
                roomSettingsNotificationsViewModel.roomNotificationLevels.getValue(NotificationLevels.SILENT)
            )
        }
    }
}

@Composable
fun RoomSettingsNotificationsLevel(
    roomSettingsNotificationsViewModel: RoomSettingsNotificationsViewModel,
    level: NotificationLevel,
) {
    val i18n = DI.current.get<I18nView>()
    val selectedRoomNotificationLevel =
        roomSettingsNotificationsViewModel.selectedRoomNotificationsLevel.collectAsState()
    val name = level.name.collectAsState()
    val explanation = level.explanation.collectAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable {
            roomSettingsNotificationsViewModel.changeSelectedRoomNotificationsLevel(
                level
            )
        }
    ) {
        RadioButton(
            selected = selectedRoomNotificationLevel.value.key == level.key,
            onClick = { roomSettingsNotificationsViewModel.changeSelectedRoomNotificationsLevel(level) },
        )
        when (level.key) {
            NotificationLevels.DEFAULT -> Icon(Icons.Default.Notifications, i18n.commonDefault())
            NotificationLevels.ALL -> Icon(Icons.Default.NotificationsActive, i18n.commonOn())
            NotificationLevels.MENTIONS -> Icon(Icons.Default.NotificationImportant, i18n.roomSettingsMentions())
            NotificationLevels.SILENT -> Icon(Icons.Default.NotificationsOff, i18n.commonOff())
        }
        Spacer(Modifier.size(10.dp))
        Text(
            text = name.value,
            style = MaterialTheme.typography.labelLarge
        )
    }
    Box(Modifier.padding(start = 48.dp)) {
        Text(
            text = explanation.value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}