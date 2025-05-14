package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.ErrorDialog
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.roomlist.create.UsersInGroup
import de.connect2x.messenger.compose.view.theme.components.ThemedFloatingActionButton
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
        val error = addMembersViewModel.error.collectAsState()
        val errorCause = addMembersViewModel.errorCause.collectAsState().value
        val i18n = DI.get<I18nView>()

        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Column {
                    Header(addMembersViewModel::back, i18n.addMembers())
                    if (error.value != null) {
                        ErrorDialog(error.value.orEmpty(), { addMembersViewModel.errorDismiss() }, errorCause = errorCause)
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
