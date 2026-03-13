package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.roomlist.create.UsersInGroup
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedFloatingActionButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersViewModel


@Composable
fun AddMembersContainer(addMembersViewModel: AddMembersViewModel) {
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        ) {
            AddMembersToRoom(addMembersViewModel)
        }
    }
}

interface AddMembersToRoomView {
    @Composable
    fun create(addMembersViewModel: AddMembersViewModel)
}

@Composable
fun AddMembersToRoom(addMembersViewModel: AddMembersViewModel) {
    DI.get<AddMembersToRoomView>().create(addMembersViewModel)
}

class AddMembersToRoomViewImpl : AddMembersToRoomView {
    @Composable
    override fun create(addMembersViewModel: AddMembersViewModel) {
        val canAddMembers = addMembersViewModel.canAddMembers.collectAsState()
        val error = addMembersViewModel.error.collectAsState().value
        val errorCause = addMembersViewModel.errorCause.collectAsState().value
        val showPreJoinHistoryWarning = addMembersViewModel.showPreJoinHistoryWarning.collectAsState().value
        val i18n = DI.get<I18nView>()
        
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Column {
                    Header(addMembersViewModel::back, i18n.addMembers())
                    if (error != null) {
                        ThemedModalDialog({ addMembersViewModel.errorDismiss() }) {
                            ModalDialogHeader {
                                Text(i18n.anErrorHasOccurred())
                            }
                            ModalDialogContent {
                                Text(error)
                                if (errorCause != null) {
                                    Text(errorCause, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            ModalDialogFooter {
                                ThemedButton(
                                    style = MaterialTheme.components.primaryButton,
                                    onClick = { addMembersViewModel.errorDismiss() },
                                ) {
                                    Text(i18n.actionOk())
                                }
                            }
                        }
                    }
                    if (showPreJoinHistoryWarning != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(all = 15.dp)) {
                            Icon(
                                Icons.Default.Info,
                                i18n.commonInformation(),
                            )
                            MiddleSpacer()
                            Text(showPreJoinHistoryWarning)
                        }
                    }
                    UsersInGroup(addMembersViewModel.potentialMembersViewModel.searchHandler)
                    SearchUsersSettings(
                        addMembersViewModel.potentialMembersViewModel,
                        onUserClick = addMembersViewModel::onUserClick
                    )
                }
                if (canAddMembers.value) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 20.dp, end = 20.dp)
                    ) {
                        ThemedFloatingActionButton(
                            expanded = true,
                            onClick = { addMembersViewModel.addMembers() },
                            text = { Text(i18n.addMembers()) },
                            icon = { Icon(Icons.Default.Check, i18n.addMembers()) },
                        )
                    }
                }
            }
        }
    }
}
