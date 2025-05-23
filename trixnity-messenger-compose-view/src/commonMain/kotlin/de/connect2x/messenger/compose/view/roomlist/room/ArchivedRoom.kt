package de.connect2x.messenger.compose.view.roomlist.room

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.common.SelectableText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.messenger.compose.view.util.TextLabel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

interface ArchivedRoomListElement {
    @Composable
    fun create(roomListElementViewModel: RoomListElementViewModel)
}

@Composable
fun ArchivedRoom(roomListElementViewModel: RoomListElementViewModel) {
    DI.get<ArchivedRoomListElement>().create(roomListElementViewModel)
}

class ArchivedRoomListElementImpl : ArchivedRoomListElement {
    @Composable
    override fun create(roomListElementViewModel: RoomListElementViewModel) {
        val i18n = DI.get<I18nView>()
        var showWarning by remember { mutableStateOf(false) }

        SpecialRoomComponent(
            roomListElementViewModel = roomListElementViewModel,
            extraInfo = {
                Spacer(Modifier.size(5.dp))
                TextLabel(i18n.commonArchived())
            }
        ) {
            Tooltip(
                tooltip = { Text(i18n.commonDelete())}
            ) {
                ThemedIconButton(
                    style = MaterialTheme.components.commonIconButton,
                    onClick = { showWarning = true },
                ) {
                    Icon(Icons.Default.Delete, i18n.commonDelete())
                }
            }
        }

        if (showWarning) {
            val roomName = roomListElementViewModel.roomName.collectAsState().value
            val isDirect = roomListElementViewModel.isDirect.collectAsState().value

            ThemedModalDialog({ showWarning = false }) {
                ModalDialogHeader {
                    Text(i18n.forgetRoomWarningHeader())
                }
                ModalDialogContent {
                    SelectableText(
                        text = i18n.formattedForgetRoomWarningBody(
                            isDirect = isDirect == true,
                            roomName = roomName
                        )
                    )
                }
                ModalDialogFooter {
                    ThemedButton(
                        style = MaterialTheme.components.commonButton,
                        onClick = { showWarning = false },
                    ) {
                        Text(i18n.actionCancel())
                    }
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        onClick = { roomListElementViewModel.forgetRoom(); showWarning = false },
                    ) {
                        Text(i18n.actionConfirm())
                    }
                }
            }
        }
    }
}
