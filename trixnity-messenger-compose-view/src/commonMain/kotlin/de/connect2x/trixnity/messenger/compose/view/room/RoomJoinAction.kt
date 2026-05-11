package de.connect2x.trixnity.messenger.compose.view.room

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.viewmodel.room.JoinRoomConfirmViewModel

interface JoinRoomActionView {
    @Composable
    fun create(viewModel: JoinRoomConfirmViewModel)
}

@Composable
fun JoinRoomAction(viewModel: JoinRoomConfirmViewModel) {
    DI.current.get<JoinRoomActionView>().create(viewModel)
}

class JoinRoomActionViewImpl : JoinRoomActionView {
    @Composable
    override fun create(viewModel: JoinRoomConfirmViewModel) {
        val action = viewModel.actionNecessary.collectAsState().value
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.align(Alignment.Center)) {
                when (action) {
                    is JoinRoomConfirmViewModel.JoinRoomAction.Impossible -> {
                        JoinRoomActionModal("Can't join room", onDismiss = action.onDismiss)
                    }

                    is JoinRoomConfirmViewModel.JoinRoomAction.Join -> JoinRoomActionModal(
                        "Please join room",
                        onConfirm = action.onJoinRoom,
                        onDismiss = action.onDismiss
                    )

                    is JoinRoomConfirmViewModel.JoinRoomAction.Knock -> JoinRoomActionModal(
                        "Please knock",
                        onConfirm = action.onKnock,
                        onDismiss = action.onDismiss
                    )

                    is JoinRoomConfirmViewModel.JoinRoomAction.Restricted -> JoinRoomActionModal(
                        "Without an invitation you need to be a member of at least one of the following rooms to join: ${action.requiredRooms.joinToString()}",
                        onDismiss = action.onDismiss
                    )

                    else -> {
                        Text("Unknown state")
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinRoomActionModal(text: String, onConfirm: (() -> Unit)? = null, onDismiss: (() -> Unit)? = null) {
    val i18n = DI.current.get<I18nView>()
    ThemedSurface(
        style = MaterialTheme.components.popup.copy(padding = PaddingValues(MaterialTheme.messengerDpConstants.large)),
    ) {
        Column {
            Text(text)
            onConfirm?.let {
                Button(onClick = it, modifier = Modifier.buttonPointerModifier()) {
                    Text(i18n.actionConfirm())
                }
            }
            onDismiss?.let {
                Button(onClick = onDismiss, modifier = Modifier.buttonPointerModifier()) {
                    Text(i18n.actionCancel())
                }
            }
        }
    }
}
