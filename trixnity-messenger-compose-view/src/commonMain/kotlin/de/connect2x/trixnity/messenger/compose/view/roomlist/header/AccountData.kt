package de.connect2x.trixnity.messenger.compose.view.roomlist.header

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.HeaderSurface
import de.connect2x.trixnity.messenger.compose.view.common.modifier.minHeaderHeight
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel

interface AccountDataView {
    @Composable fun create(roomListViewModel: RoomListViewModel)
}

@Composable
fun AccountData(roomListViewModel: RoomListViewModel) {
    DI.get<AccountDataView>().create(roomListViewModel)
}

class AccountDataViewImpl : AccountDataView {
    @Composable
    override fun create(roomListViewModel: RoomListViewModel) {
        val accountViewModel = roomListViewModel.accountViewModel

        HeaderSurface {
            Row(Modifier.minHeaderHeight(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccountAvatar(accountViewModel)
                    if (roomListViewModel.closeProfileNeeded) {
                        CloseProfile({ roomListViewModel.closeProfile() })
                    }
                    ShowSearch(roomListViewModel)
                    AccountOptions(accountViewModel, roomListViewModel)
                }
            }
        }
    }
}
