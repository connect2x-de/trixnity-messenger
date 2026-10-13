package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import de.connect2x.trixnity.messenger.viewmodel.room.call.CallControlViewModel

@Composable
@TrixnityMessengerCallApi
fun CallControlButton(viewModel: CallControlViewModel) {
    val state by viewModel.state.collectAsState()
    val canCall by viewModel.canJoinCall.collectAsState()
    val i18n = DI.get<I18nView>()
    val error = (state as? CallControlViewModel.State.Error)?.error

    val tooltipText =
        when {
            error != null -> i18n.callStartWithError(error)
            else -> i18n.callStart()
        }

    val callButtonColor =
        when {
            state == CallControlViewModel.State.Active -> MaterialTheme.messengerColors.success
            state == CallControlViewModel.State.Joined -> MaterialTheme.colorScheme.error
            error != null -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    if (canCall) {
        Tooltip(tooltip = { Text(tooltipText) }) {
            ThemedIconButton(
                style = MaterialTheme.components.commonIconButton,
                onClick = {
                    when (state) {
                        CallControlViewModel.State.Joined -> viewModel.leave()
                        else -> viewModel.join()
                    }
                },
            ) {
                BadgedBox(
                    badge = {
                        if (error != null) {
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
                    Icon(imageVector = Icons.Default.Call, contentDescription = tooltipText, tint = callButtonColor)
                }
            }
        }
    }
}
