package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.HeaderBackButtonType.BACK
import de.connect2x.trixnity.messenger.compose.view.common.HeaderBackButtonType.CLOSE
import de.connect2x.trixnity.messenger.compose.view.util.DevInfoButton
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel

@Composable
internal fun SettingsPane(
    viewModel: RoomSettingsViewModel,
    isSinglePane: Boolean,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val error = viewModel.error.collectAsState().value
    val scroll = rememberScrollState()
    ExtrasPaneHeader(
        title,
        error,
        viewModel::close,
        if (isSinglePane) BACK else CLOSE,
        { DevInfoButton(viewModel::openDevInfoView) },
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.verticalScroll(scroll).padding(PaddingValues(vertical = 0.dp, horizontal = 20.dp)),
                Arrangement.spacedBy(20.dp),
                content = content,
            )
            VerticalScrollbar(Modifier.align(Alignment.CenterEnd).fillMaxHeight(), scroll)
        }
    }
}

@Composable
internal fun SettingsIdentity(
    viewModel: RoomSettingsViewModel,
    name: @Composable () -> Unit,
    topic: @Composable () -> Unit,
) {
    ChangeRoomAvatar(viewModel.changeRoomAvatarViewModel)
    name()
    topic()
    val aliases = viewModel.roomSettingsAliasViewModel
    if (aliases.showRoomAliasSettings.collectAsState().value) RoomSettingsAlias(aliases)
}

@Composable
internal fun SettingsLeave(viewModel: RoomSettingsViewModel) {
    RoomSettingsLeaveRoom(viewModel)
    if (viewModel.leaveRoomWarningOpen.collectAsState().value) RoomSettingsLeaveRoomWarning(viewModel)
}
