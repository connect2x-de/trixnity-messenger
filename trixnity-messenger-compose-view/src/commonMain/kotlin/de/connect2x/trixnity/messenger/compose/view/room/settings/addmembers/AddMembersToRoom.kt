package de.connect2x.trixnity.messenger.compose.view.room.settings.addmembers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.ErrorDialog
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.settings.SearchUsersSettings
import de.connect2x.trixnity.messenger.compose.view.search.UsersInGroup
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersViewModel

@Composable
fun AddMembersContainer(addMembersViewModel: AddMembersViewModel) {
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxHeight().align(Alignment.CenterEnd)) { AddMembersToRoom(addMembersViewModel) }
    }
}

interface AddMembersToRoomView {
    @Composable fun create(addMembersViewModel: AddMembersViewModel)
}

@Composable
fun AddMembersToRoom(addMembersViewModel: AddMembersViewModel) {
    DI.get<AddMembersToRoomView>().create(addMembersViewModel)
}

class AddMembersToRoomViewImpl : AddMembersToRoomView {
    @Composable
    override fun create(addMembersViewModel: AddMembersViewModel) {
        val error = addMembersViewModel.error.collectAsState().value
        val errorCause = addMembersViewModel.errorCause.collectAsState().value
        val i18n = DI.get<I18nView>()

        Box(Modifier.fillMaxSize()) {
            Column {
                Header(addMembersViewModel::back, i18n.addMembers())
                UndecryptableHistoryInfo(addMembersViewModel)
                UsersInGroup(addMembersViewModel.potentialMembersViewModel.searchHandler)
                SearchUsersSettings(
                    addMembersViewModel.potentialMembersViewModel,
                    onUserClick = addMembersViewModel::onUserClick,
                )
            }
            AddMembersFloatingButton(addMembersViewModel)
            ErrorDialog(error, errorCause, onDismiss = { addMembersViewModel.errorDismiss() })
        }
    }
}
