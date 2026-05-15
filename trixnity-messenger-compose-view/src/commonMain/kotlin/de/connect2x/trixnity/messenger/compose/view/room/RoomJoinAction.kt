package de.connect2x.trixnity.messenger.compose.view.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.viewmodel.room.JoinRoomActionViewModel

interface JoinRoomActionView {
    @Composable
    fun create(viewModel: JoinRoomActionViewModel)
}

@Composable
fun JoinRoomAction(viewModel: JoinRoomActionViewModel) {
    DI.current.get<JoinRoomActionView>().create(viewModel)
}

class JoinRoomActionViewImpl : JoinRoomActionView {
    @Composable
    override fun create(viewModel: JoinRoomActionViewModel) {
        val action = viewModel.actionNecessary.collectAsState().value
        val error = viewModel.error.collectAsState().value
        val i18n = DI.current.get<I18nView>()
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.align(Alignment.Center)) {
                when (action) {
                    is JoinRoomActionViewModel.JoinRoomAction.Impossible -> {
                        JoinRoomActionModal(
                            i18n.joinRoomActionImpossible(),
                            error,
                            onDismiss = action.onDismiss
                        )
                    }

                    is JoinRoomActionViewModel.JoinRoomAction.Join -> JoinRoomActionModal(
                        i18n.joinRoomActionJoin(),
                        error,
                        onConfirm = action.onJoinRoom,
                        onDismiss = action.onDismiss
                    )

                    is JoinRoomActionViewModel.JoinRoomAction.Knock -> JoinRoomActionModal(
                        i18n.joinRoomActionKnock(),
                        error,
                        onConfirm = action.onKnock,
                        onDismiss = action.onDismiss
                    )

                    is JoinRoomActionViewModel.JoinRoomAction.Restricted -> JoinRoomActionModal(
                        i18n.joinRoomActionRestricted(action.requiredRooms), error, onDismiss = action.onDismiss
                    )

                    is JoinRoomActionViewModel.JoinRoomAction.AcceptInvitation -> {
                        JoinRoomActionModal(
                            i18n.joinRoomActionAcceptInvite(),
                            error,
                            onConfirm = action.onAcceptInvite,
                            onDismiss = action.onDismiss
                        )
                    }

                    null -> {}
                }
            }
        }
    }
}

@Composable
private fun JoinRoomActionModal(
    text: String,
    error: String?,
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val i18n = DI.current.get<I18nView>()
    Box(Modifier.fillMaxWidth(0.5f)) {
        ThemedSurface(
            style = MaterialTheme.components.details,
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.messengerDpConstants.middle),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text)
                error?.let {
                    SmallSpacer()
                    ThemedSurface(style = MaterialTheme.components.popup) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(MaterialTheme.messengerDpConstants.small)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                i18n.commonWarning(),
                                tint = MaterialTheme.messengerColors.warning
                            )
                            MiddleSpacer()
                            Text(error)
                        }
                    }
                }
                MiddleSpacer()
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.messengerDpConstants.small)) {
                    onDismiss?.let {
                        ThemedButton(
                            onClick = onDismiss,
                            modifier = Modifier.buttonPointerModifier(),
                            style = MaterialTheme.components.secondaryButton
                        ) {
                            Text(i18n.actionCancel())
                        }
                        onConfirm?.let {
                            ThemedButton(
                                onClick = it,
                                modifier = Modifier.buttonPointerModifier(),
                                style = MaterialTheme.components.primaryButton
                            ) {
                                Text(i18n.actionConfirm())
                            }
                        }
                    }
                }
            }
        }
    }
}
