package de.connect2x.messenger.compose.view.roomlist.header

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel

interface ShowSpacesView {
    @Composable
    fun create(roomListViewModel: RoomListViewModel)
}

@Composable
fun ShowSpaces(roomListViewModel: RoomListViewModel) {
    DI.current.get<ShowSpacesView>().create(roomListViewModel)
}

class ShowSpacesViewImpl : ShowSpacesView {
    @Composable
    override fun create(roomListViewModel: RoomListViewModel) {
        val i18n = DI.current.get<I18nView>()
        val spaces = roomListViewModel.spaces.collectAsState()
        val showSpaces = roomListViewModel.showSpaces.collectAsState()

        if (spaces.value.isNotEmpty()) {
            if (showSpaces.value) {
                Tooltip({ TooltipText(i18n.accountDeactivateFilter()) }) {
                    IconButton({
                        roomListViewModel.showSpaces.value = false
                    }, Modifier.buttonPointerModifier()) {
                        Icon(Icons.Default.FilterListOff, i18n.accountDeactivateFilter())
                    }
                }
            } else {
                Tooltip({ TooltipText(i18n.accountSelectFilter()) }) {
                    IconButton({
                        roomListViewModel.showSpaces.value = true
                    }, Modifier.buttonPointerModifier()) {
                        Icon(Icons.Default.FilterList, i18n.accountSelectFilter())
                    }
                }
            }
        }
    }
}
