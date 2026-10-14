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
import de.connect2x.trixnity.messenger.viewmodel.room.call.CallControlViewModel

@Composable
@TrixnityMessengerCallApi
fun CallControlButton(viewModel: CallControlViewModel) {
    val state = viewModel.state.collectAsState().value
    val errorMessage = (state as? CallControlViewModel.State.Error)?.message

    val colorAndIcon =
        when (state) {
            is CallControlViewModel.State.CallAvailable if state.hasParticipants ->
                MaterialTheme.messengerColors.success to Icons.Default.Call
            is CallControlViewModel.State.CallAvailable ->
                MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.Call
            is CallControlViewModel.State.Joining -> MaterialTheme.colorScheme.error to Icons.Default.Call
            is CallControlViewModel.State.Join -> MaterialTheme.colorScheme.error to Icons.Default.CallEnd
            CallControlViewModel.State.Leaving -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.CallEnd
            is CallControlViewModel.State.Error -> MaterialTheme.colorScheme.error to Icons.Default.Call
            CallControlViewModel.State.NoCall -> null
            null -> null
        }

    if (colorAndIcon != null) {
        val (color, icon) = colorAndIcon
        ThemedIconButton(
            style = MaterialTheme.components.commonIconButton,
            enabled = state is CallControlViewModel.State.CallAvailable || state is CallControlViewModel.State.Join,
            onClick = {
                when (state) {
                    is CallControlViewModel.State.CallAvailable -> viewModel.join()
                    CallControlViewModel.State.Join -> viewModel.leave()
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
