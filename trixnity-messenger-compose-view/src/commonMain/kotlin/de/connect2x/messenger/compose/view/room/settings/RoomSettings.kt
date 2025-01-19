package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.HeaderBackButtonType.BACK
import de.connect2x.messenger.compose.view.common.HeaderBackButtonType.CLOSE
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent


@Composable
fun RoomSettingsContainer(roomSettingsViewModel: RoomSettingsViewModel, isSinglePane: Boolean) {
    Box(Modifier.fillMaxWidth().clickable(enabled = false) {}) {
        Box(
            Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        ) {
            RoomSettings(roomSettingsViewModel, isSinglePane)
        }
    }
}

interface RoomSettingsView {
    @Composable
    fun create(roomSettingsViewModel: RoomSettingsViewModel, isSinglePane: Boolean)
}

@Composable
fun RoomSettings(roomSettingsViewModel: RoomSettingsViewModel, isSinglePane: Boolean) {
    DI.get<RoomSettingsView>().create(roomSettingsViewModel, isSinglePane)
}

class RoomSettingsViewImpl : RoomSettingsView {
    @Composable
    override fun create(roomSettingsViewModel: RoomSettingsViewModel, isSinglePane: Boolean) {
        val i18n = DI.get<I18nView>()
        val error = roomSettingsViewModel.error.collectAsState().value
        val leaveRoomWarningOpen = roomSettingsViewModel.leaveRoomWarningOpen.collectAsState().value
        val scroll = rememberScrollState()
        val joinRule = roomSettingsViewModel.roomSettingsJoinRulesViewModel.joinRule.collectAsState().value
        ExtrasPaneHeader(
            i18n.roomSettings(),
            error,
            { roomSettingsViewModel.close() },
            if (isSinglePane) BACK else CLOSE,
        ) {
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
