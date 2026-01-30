package de.connect2x.trixnity.messenger.compose.view.roomlist.header

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel

interface ShowSearchView {
    @Composable
    fun create(roomListViewModel: RoomListViewModel)
}

@Composable
fun ShowSearch(roomListViewModel: RoomListViewModel) {
    DI.get<ShowSearchView>().create(roomListViewModel)
}

class ShowSearchViewImpl : ShowSearchView {
    @Composable
    override fun create(roomListViewModel: RoomListViewModel) {
        val i18n = DI.get<I18nView>()
        val showSearch = roomListViewModel.showSearch.collectAsState()

        if (showSearch.value) {
            Tooltip({ Text(i18n.accountDeactivateSearch()) }) {
                ThemedIconButton(
                    style = MaterialTheme.components.commonIconButton,
                    onClick = { roomListViewModel.showSearch.value = false },
                ) {
                    Icon(Icons.Default.SearchOff, i18n.accountDeactivateSearch())
                }
            }
        } else {
            Tooltip({ Text(i18n.accountActivateSearch()) }) {
                ThemedIconButton(
                    style = MaterialTheme.components.commonIconButton,
                    onClick = { roomListViewModel.showSearch.value = true },
                ) {
                    Icon(Icons.Default.Search, i18n.accountActivateSearch())
                }
            }
        }
    }
}
