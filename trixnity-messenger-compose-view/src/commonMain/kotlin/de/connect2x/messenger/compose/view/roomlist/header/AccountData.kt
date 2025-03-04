package de.connect2x.messenger.compose.view.roomlist.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.root.IsSinglePane
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
        val density = LocalDensity.current

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Row(
                Modifier.onGloballyPositioned { coordinates ->
                    val newHeaderHeight = with(density) { coordinates.size.height.toDp() - 1.toDp() }
                    headerHeightFlow.value = maxOf(headerHeight, newHeaderHeight)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AccountAvatar(accountViewModel)
                    CloseProfile(roomListViewModel)
                    ShowSearch(roomListViewModel)
                    AccountOptions(accountViewModel, roomListViewModel)
                }
                // If we have a multi-pane view, we will display an invisible text that has the function of forcing the
                // three header elements to the same height.
                if (!IsSinglePane.current) {
                    Text(
                        text = " ",
                        style = MaterialTheme.typography.labelMedium
                            .copy(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.height(headerHeight)
                    )
                }
            }
        }
    }
}
