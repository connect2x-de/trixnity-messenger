package de.connect2x.messenger.compose.view.roomlist.header

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.theme.MaxHeaderHeight
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel

interface AccountDataView {
    @Composable
    fun create(roomListViewModel: RoomListViewModel)
}

@Composable
fun AccountData(roomListViewModel: RoomListViewModel) {
    DI.get<AccountDataView>().create(roomListViewModel)
}

class AccountDataViewImpl : AccountDataView {
    @Composable
    override fun create(roomListViewModel: RoomListViewModel) {
        val accountViewModel = roomListViewModel.accountViewModel
        val headerHeightFlow = MaxHeaderHeight.current
        val headerHeight = headerHeightFlow.collectAsState().value

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Row(Modifier.onGloballyPositioned { coordinates ->
                val size = coordinates.size
                headerHeightFlow.value = maxOf(headerHeight, size.height)
            }, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = " ",
                    modifier = Modifier.height(with(LocalDensity.current) { headerHeight.toDp() })
                )
                Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp).fillMaxWidth()) {
                    AccountAvatar(accountViewModel)
                    CloseProfile(roomListViewModel)
                    ShowSearch(roomListViewModel)
                    AccountOptions(accountViewModel, roomListViewModel)
                }
            }
        }
    }
}
