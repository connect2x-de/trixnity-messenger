package de.connect2x.trixnity.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.ROOM_LIST_WEIGHT
import de.connect2x.trixnity.messenger.compose.view.ROOM_WEIGHT
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.room.RoomSwitch
import de.connect2x.trixnity.messenger.compose.view.roomlist.RoomListSwitch
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedVerticalDivider
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter
import de.connect2x.trixnity.messenger.viewmodel.util.toFlow
import kotlinx.coroutines.flow.map

interface MessengerView {
    @Composable fun create(mainViewModel: MainViewModel, isSinglePane: Boolean)
}

@Composable
fun Messenger(mainViewModel: MainViewModel, isSinglePane: Boolean) {
    DI.get<MessengerView>().create(mainViewModel, isSinglePane)
}

class MessengerViewImpl : MessengerView {
    @Composable
    override fun create(mainViewModel: MainViewModel, isSinglePane: Boolean) {
        val isRoomShown =
            remember {
                    mainViewModel.roomRouterStack.toFlow().map { it.active.configuration !is RoomRouter.Config.None }
                }
                .collectAsState(initial = false)
                .value
        Row(modifier = Modifier.fillMaxSize()) {

            // Room List
            if (!isRoomShown || !isSinglePane)
                Box(modifier = Modifier.weight(if (isSinglePane) 1F else ROOM_LIST_WEIGHT)) {
                    RoomListSwitch(mainViewModel)
                }

            // Pane Divider
            if (!isSinglePane) {
                ThemedVerticalDivider(Modifier.fillMaxHeight().align(Alignment.CenterVertically))
            }

            // Room Pane
            if (isRoomShown || !isSinglePane)
                Box(modifier = Modifier.weight(if (isSinglePane) 1F else ROOM_WEIGHT)) {
                    RoomSwitch(mainViewModel.roomRouterStack)
                }
        }
    }
}
