package de.connect2x.messenger.compose.view.roomlist.room

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import net.folivo.trixnity.core.model.RoomId

interface RoomListElementContainerView {
    @Composable
    fun LazyItemScope.create(
        roomId: RoomId,
        roomListViewModel: RoomListViewModel,
        roomListElementViewModel: RoomListElementViewModel
    )
}

@Composable
fun LazyItemScope.RoomListElementContainer(
    roomId: RoomId,
    roomListViewModel: RoomListViewModel,
    roomListElementViewModel: RoomListElementViewModel,
) {
    with(DI.get<RoomListElementContainerView>()) { create(roomId, roomListViewModel, roomListElementViewModel) }
}

class RoomListElementContainerViewImpl : RoomListElementContainerView {
    @Composable
    override fun LazyItemScope.create(
        roomId: RoomId,
        roomListViewModel: RoomListViewModel,
        roomListElementViewModel: RoomListElementViewModel,
    ) {
        val selectedRoomId = roomListViewModel.selectedRoomId.collectAsState().value
        val roomName = roomListElementViewModel.roomName.collectAsState().value
        val isInvite = roomListElementViewModel.isInvite.collectAsState().value
        Modifier
            .fillMaxWidth()
        Box(
            Modifier.animateItem(
                fadeInSpec = null,
                fadeOutSpec = null,
                placementSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = IntOffset.VisibilityThreshold
                )
            )
                .clickable(enabled = roomName != null && (isInvite == null || isInvite == false)) {
                    roomListViewModel.selectRoom(roomId)
                }
                .background(if (roomId == selectedRoomId) { MaterialTheme.components.roomListSelection.color } else Color.Unspecified)
                .buttonPointerModifier(enabled = isInvite == null || isInvite == false)
        ) {
            CompositionLocalProvider(LocalContentColor provides if (roomId == selectedRoomId) MaterialTheme.components.roomListSelection.contentColor else LocalContentColor.current) {
                RoomListElement(roomListViewModel, roomListElementViewModel)
            }
        }
        HorizontalDivider(Modifier.fillMaxWidth().width(1.dp).padding(horizontal = 10.dp))
    }
}
