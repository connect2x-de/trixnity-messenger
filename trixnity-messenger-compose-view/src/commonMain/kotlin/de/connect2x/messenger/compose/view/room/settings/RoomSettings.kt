package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent

@Composable
fun RoomSettingsContainer(roomSettingsViewModel: RoomSettingsViewModel, isTwoPane: Boolean) {
    Box(Modifier.fillMaxWidth().clickable(enabled = false) {}) {
        Box(
            Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        ) {
            RoomSettings(roomSettingsViewModel, isTwoPane)
        }
    }
}

interface RoomSettingsView {
    @Composable
    fun create(roomSettingsViewModel: RoomSettingsViewModel, isTwoPane: Boolean)
}

@Composable
fun RoomSettings(roomSettingsViewModel: RoomSettingsViewModel, isTwoPane: Boolean) {
    DI.get<RoomSettingsView>().create(roomSettingsViewModel, isTwoPane)
}

class RoomSettingsViewImpl : RoomSettingsView {
    @Composable
    override fun create(roomSettingsViewModel: RoomSettingsViewModel, isTwoPane: Boolean) {
        val i18n = DI.get<I18nView>()
        val error = roomSettingsViewModel.error.collectAsState().value
        val leaveRoomWarningOpen = roomSettingsViewModel.leaveRoomWarningOpen.collectAsState().value
        val scroll = rememberScrollState()
        val joinRule = roomSettingsViewModel.roomSettingsJoinRulesViewModel.joinRule.collectAsState().value

        Box(
            Modifier.fillMaxSize(),
        ) {
            Column {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { roomSettingsViewModel.close() }, Modifier.buttonPointerModifier()) {
                                if (isTwoPane) Icon(Icons.AutoMirrored.Default.ArrowBack, i18n.commonBack())
                                else Icon(Icons.Default.Close, i18n.commonClose())
                            }
                            Spacer(Modifier.size(10.dp))
                            Text(
                                text = i18n.roomSettings().capitalize(Locale.current),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
                        if (error != null) {
                            ErrorView(error)
                        }
                    }
                }
                Box(
                    Modifier.fillMaxSize()
                ) {
                    Column(
                        Modifier
                            .verticalScroll(scroll)
                            .padding(PaddingValues(vertical = 0.dp, horizontal = 20.dp))
                    ) {
                        Spacer(Modifier.size(20.dp))
                        val changeRoomAvatarViewModel = roomSettingsViewModel.changeRoomAvatarViewModel
                        ChangeRoomAvatar(changeRoomAvatarViewModel)
                        Spacer(Modifier.size(20.dp))
                        val roomNameViewModel = roomSettingsViewModel.roomSettingsNameViewModel
                        RoomSettingsName(roomNameViewModel)
                        Spacer(Modifier.size(20.dp))
                        val roomTopicViewModel = roomSettingsViewModel.roomSettingsTopicViewModel
                        RoomSettingsTopic(roomTopicViewModel)
                        Spacer(Modifier.size(20.dp))
                        val roomAliasViewModel = roomSettingsViewModel.roomSettingsAliasViewModel
                        val showRoomAliasSettings = roomAliasViewModel.showRoomAliasSettings.collectAsState().value
                        if (showRoomAliasSettings) {
                            RoomSettingsAlias(roomAliasViewModel)
                            Spacer(Modifier.size(20.dp))
                        }
                        if (joinRule == JoinRulesEventContent.JoinRule.Public) {
                            HorizontalDivider()
                            Spacer(Modifier.size(20.dp))
                            RoomSettingsSecurity(roomSettingsViewModel.roomSettingsSecurityViewModel)
                            Spacer(Modifier.size(20.dp))
                        }
                        HorizontalDivider()
                        Spacer(Modifier.size(20.dp))
                        RoomSettingsNotifications(roomSettingsViewModel.roomSettingsNotificationsViewModel)
                        Spacer(Modifier.size(20.dp))
                        HorizontalDivider()
                        Spacer(Modifier.size(20.dp))
                        RoomSettingsHistoryVisibility(roomSettingsViewModel)
                        Spacer(Modifier.size(20.dp))
                        HorizontalDivider()
                        Spacer(Modifier.size(20.dp))
                        RoomSettingsJoinRules(roomSettingsViewModel)
                        Spacer(Modifier.size(20.dp))
                        HorizontalDivider()
                        Spacer(Modifier.size(10.dp))
                        RoomSettingsMemberList(roomSettingsViewModel)
                        Spacer(Modifier.size(10.dp))
                        HorizontalDivider()
                        Spacer(Modifier.size(20.dp))
                        RoomSettingsExportRoom(roomSettingsViewModel)
                        Spacer(Modifier.size(20.dp))
                        RoomSettingsLeaveRoom(roomSettingsViewModel)
                        Spacer(Modifier.size(20.dp))
                        if (leaveRoomWarningOpen) RoomSettingsLeaveRoomWarning(roomSettingsViewModel)
                    }
                    VerticalScrollbar(
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        scroll,
                    )
                }
            }
        }
    }
}
