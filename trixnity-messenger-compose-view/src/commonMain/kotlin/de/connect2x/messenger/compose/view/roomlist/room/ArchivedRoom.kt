package de.connect2x.messenger.compose.view.roomlist.room

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.common.MessengerModal
import de.connect2x.messenger.compose.view.common.MessengerModalContent
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
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

            Dialog(onDismissRequest = { showWarning = false }) {
                MessengerModal(
                    onDismiss = { showWarning = false },
                    width = 500.dp,
                    title = i18n.forgetRoomWarningHeader()
                ) {
                    MessengerModalContent {
                        Text(
                            text = i18n.formattedForgetRoomWarningBody(
                                isDirect = isDirect == true,
                                roomName = roomName
                            ),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.size(20.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(IntrinsicSize.Min)
                        ) {
                            Box(Modifier.weight(1.0f, fill = true), contentAlignment = Alignment.Center) {
                                ThemedButton(
                                    style = MaterialTheme.components.primaryButton,
                                    onClick = { roomListElementViewModel.forgetRoom(); showWarning = false },
                                    modifier = Modifier.width(250.dp),
                                ) {
                                    Text(i18n.commonConfirm().capitalize(Locale.current))
                                }
                            }
                            Spacer(Modifier.size(20.dp))
                            Box(Modifier.weight(1.0f, fill = true), contentAlignment = Alignment.Center) {
                                ThemedButton(
                                    style = MaterialTheme.components.commonButton,
                                    onClick = { showWarning = false },
                                    modifier = Modifier.width(250.dp),
                                ) {
                                    Text(i18n.commonBack().capitalize(Locale.current))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
