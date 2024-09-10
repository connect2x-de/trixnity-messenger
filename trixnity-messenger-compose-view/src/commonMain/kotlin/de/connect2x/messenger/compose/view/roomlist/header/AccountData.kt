package de.connect2x.messenger.compose.view.roomlist.header

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel


interface AccountDataView {
    @Composable
    fun create(roomListViewModel: RoomListViewModel)
}

@Composable
fun AccountData(roomListViewModel: RoomListViewModel) {
    DI.current.get<AccountDataView>().create(roomListViewModel)
}

class AccountDataViewImpl : AccountDataView {
    @Composable
    override fun create(roomListViewModel: RoomListViewModel) {
        val accountViewModel = roomListViewModel.accountViewModel
        Surface(
            Modifier
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Row(
                Modifier
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccountAvatar(accountViewModel)
                CloseProfile(roomListViewModel)
                ShowSpaces(roomListViewModel)
                ShowSearch(roomListViewModel)
                AccountOptions(accountViewModel, roomListViewModel)
            }
        }
    }
}
