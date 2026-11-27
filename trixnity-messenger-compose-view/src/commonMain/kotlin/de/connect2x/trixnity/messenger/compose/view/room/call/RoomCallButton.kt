package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallButtonViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallButtonViewModel.State

@Composable
@TrixnityMessengerCallApi
fun RoomCallButton(viewModel: RoomCallButtonViewModel) {
    val state = viewModel.state.collectAsState().value ?: return
    val errorMessage = (state as? State.Error)?.message

    val (color, icon) =
        when (state) {
            State.ActiveCall -> MaterialTheme.messengerColors.success to Icons.Default.Call
            State.CallInAnotherRoom -> null to Icons.Default.Call
            is State.CallAvailable if state.hasParticipants ->
                MaterialTheme.messengerColors.success to Icons.Default.Call
            is State.CallAvailable -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.Call
            State.Joining -> MaterialTheme.colorScheme.error to Icons.Default.Call
            State.Join -> MaterialTheme.colorScheme.error to Icons.Default.CallEnd
            State.Leaving -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.CallEnd
            is State.Error -> MaterialTheme.colorScheme.error to Icons.Default.Call
            State.NoCall -> return
        }

    val i18n = DI.get<I18nView>()
    val label =
        when (state) {
            State.ActiveCall -> i18n.callRestore()
            State.CallInAnotherRoom -> i18n.callAlreadyInProgress()
            is State.CallAvailable -> i18n.callStart()
            State.Joining -> i18n.callJoining()
            State.Join,
            State.Leaving -> i18n.callLeave()
            is State.Error -> state.message
            State.NoCall -> return
        }
    Tooltip(tooltip = { Text(label) }) {
        ThemedIconButton(
            style = MaterialTheme.components.commonIconButton,
            enabled = state is State.CallAvailable || state == State.ActiveCall || state == State.Join,
            onClick = {
                when (state) {
                    is State.CallAvailable,
                    State.ActiveCall -> viewModel.join()
                    State.Join -> viewModel.leave()
                    else -> {}
                }
            },
        ) {
            BadgedBox(
                badge = {
                    if (errorMessage != null) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                            )
                        }
                    }
                }
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = color ?: LocalContentColor.current)
            }
        }
    }
}
