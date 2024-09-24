package de.connect2x.messenger.compose.view.roomlist.header

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
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
            Tooltip(
                tooltip = { TooltipText(i18n.accountDeactivateSearch()) }) {
                IconButton(
                    onClick = { roomListViewModel.showSearch.value = false },
                ) {
                    Icon(Icons.Default.SearchOff, i18n.accountActivateSearch())
                }
            }
        } else {
            Tooltip(
                tooltip = { TooltipText(i18n.accountActivateSearch()) }) {
                IconButton(
                    onClick = { roomListViewModel.showSearch.value = true },
                ) {
                    Icon(Icons.Default.Search, i18n.accountActivateSearch())
                }
            }
        }
    }
}
