package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.common.RadioSetting
import de.connect2x.messenger.compose.view.common.RadioSettingOption
import de.connect2x.messenger.compose.view.common.SmallLoadingSpinner
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.NotificationLevels
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsNotificationsViewModel

interface RoomSettingsNotificationsView {
    @Composable
    fun create(roomSettingsNotificationsViewModel: RoomSettingsNotificationsViewModel)
}

@Composable
fun RoomSettingsNotifications(roomSettingsNotificationsViewModel: RoomSettingsNotificationsViewModel) {
    DI.get<RoomSettingsNotificationsView>().create(roomSettingsNotificationsViewModel)
}

class RoomSettingsNotificationsViewImpl : RoomSettingsNotificationsView {
    @Composable
    override fun create(roomSettingsNotificationsViewModel: RoomSettingsNotificationsViewModel) {
        val i18n = DI.get<I18nView>()
        val isLoading = roomSettingsNotificationsViewModel.isNotificationsLevelLoading.collectAsState().value
        val selectedLevel = roomSettingsNotificationsViewModel.selectedRoomNotificationsLevel.collectAsState().value
        val selectedLevelName = selectedLevel.name.collectAsState().value
        val selectedLevelExplanation = selectedLevel.explanation.collectAsState().value
        val roomNotificationLevels = roomSettingsNotificationsViewModel.roomNotificationLevels
        val roomNotificationLevelDefault = roomNotificationLevels.getValue(NotificationLevels.DEFAULT)
        val roomNotificationLevelAll = roomNotificationLevels.getValue(NotificationLevels.ALL)
        val roomNotificationLevelMentions = roomNotificationLevels.getValue(NotificationLevels.MENTIONS)
        val roomNotificationLevelSilent = roomNotificationLevels.getValue(NotificationLevels.SILENT)

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = i18n.commonNotifications().capitalize(Locale.current),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.size(10.dp))
            RadioSetting(
                title = {
                    if (isLoading) {
                        SmallLoadingSpinner()
                    } else {
                        Tooltip(tooltip = { TooltipText { selectedLevelExplanation } }) {
                            Text(selectedLevelName)
                        }
                    }
                },
                options = mapOf(
                    roomNotificationLevelDefault to RadioSettingOption(
                        roomNotificationLevelDefault.name.collectAsState().value,
                        roomNotificationLevelDefault.explanation.collectAsState().value
                    ),
                    roomNotificationLevelAll to RadioSettingOption(
                        roomNotificationLevelAll.name.collectAsState().value,
                        roomNotificationLevelAll.explanation.collectAsState().value
                    ),
                    roomNotificationLevelMentions to RadioSettingOption(
                        roomNotificationLevelMentions.name.collectAsState().value,
                        roomNotificationLevelMentions.explanation.collectAsState().value
                    ),
                    roomNotificationLevelSilent to RadioSettingOption(
                        roomNotificationLevelSilent.name.collectAsState().value,
                        roomNotificationLevelSilent.explanation.collectAsState().value
                    )
                ),
                value = selectedLevel,
                set = { roomSettingsNotificationsViewModel.changeSelectedRoomNotificationsLevel(newLevel = it) },
                icon = when (selectedLevel.key) {
                    NotificationLevels.DEFAULT -> Icons.Default.Notifications
                    NotificationLevels.ALL -> Icons.Default.NotificationsActive
                    NotificationLevels.MENTIONS -> Icons.Default.NotificationImportant
                    NotificationLevels.SILENT -> Icons.Default.NotificationsOff
                }
            )
        }
    }
}
