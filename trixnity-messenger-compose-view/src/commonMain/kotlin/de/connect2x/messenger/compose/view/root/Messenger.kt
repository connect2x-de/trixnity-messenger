package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.ROOM_LIST_WEIGHT
import de.connect2x.messenger.compose.view.ROOM_WEIGHT
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.room.RoomSwitch
import de.connect2x.messenger.compose.view.roomlist.RoomListSwitch
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel


interface MessengerView {
    @Composable
    fun create(mainViewModel: MainViewModel, isSinglePane: Boolean)
}

@Composable
fun Messenger(mainViewModel: MainViewModel, isSinglePane: Boolean) {
    DI.get<MessengerView>().create(mainViewModel, isSinglePane)
}

class MessengerViewImpl : MessengerView {
    @Composable
    override fun create(mainViewModel: MainViewModel, isSinglePane: Boolean) {
        val isRoomShown = mainViewModel.isRoomShown.collectAsState().value
        Row(modifier = Modifier.fillMaxSize()) {

            // Room List
            if (!(isRoomShown && isSinglePane)) Box(
                modifier = Modifier
                    .weight(if (isSinglePane) 1F else ROOM_LIST_WEIGHT)
            ) {
                RoomListSwitch(mainViewModel)
            }

            // Pane Divider
            if (isRoomShown && !isSinglePane) VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
            )

            // Room Pane
            if (isRoomShown || !isSinglePane) Box(
                modifier = Modifier
                    .weight(if (isSinglePane) 1F else ROOM_WEIGHT)
            ) {
                RoomSwitch(mainViewModel.roomRouterStack)
            }
        }
    }
}
