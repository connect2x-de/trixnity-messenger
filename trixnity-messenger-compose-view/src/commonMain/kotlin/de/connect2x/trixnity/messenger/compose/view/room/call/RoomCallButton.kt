package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallButtonViewModel

@Composable
@TrixnityMessengerCallApi
fun RoomCallButton(viewModel: RoomCallButtonViewModel) {
    val state = viewModel.state.collectAsState().value
    val errorMessage = (state as? RoomCallButtonViewModel.State.Error)?.message

    val colorAndIcon =
        when (state) {
            is RoomCallButtonViewModel.State.CallAvailable if state.hasParticipants ->
                MaterialTheme.messengerColors.success to Icons.Default.Call
            is RoomCallButtonViewModel.State.CallAvailable ->
                MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.Call
            is RoomCallButtonViewModel.State.Joining -> MaterialTheme.colorScheme.error to Icons.Default.Call
            is RoomCallButtonViewModel.State.Join -> MaterialTheme.colorScheme.error to Icons.Default.CallEnd
            RoomCallButtonViewModel.State.Leaving -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.CallEnd
            is RoomCallButtonViewModel.State.Error -> MaterialTheme.colorScheme.error to Icons.Default.Call
            RoomCallButtonViewModel.State.NoCall -> null
            null -> null
        }

    if (colorAndIcon != null) {
        val (color, icon) = colorAndIcon
        ThemedIconButton(
            style = MaterialTheme.components.commonIconButton,
            enabled =
                state is RoomCallButtonViewModel.State.CallAvailable || state is RoomCallButtonViewModel.State.Join,
            onClick = {
                when (state) {
                    is RoomCallButtonViewModel.State.CallAvailable -> viewModel.join()
                    RoomCallButtonViewModel.State.Join -> viewModel.leave()
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
                Icon(imageVector = icon, contentDescription = errorMessage, tint = color)
            }
        }
    }
}
