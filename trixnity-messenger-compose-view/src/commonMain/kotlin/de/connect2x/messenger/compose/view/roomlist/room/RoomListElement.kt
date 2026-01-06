package de.connect2x.messenger.compose.view.roomlist.room

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedHorizontalDivider
import de.connect2x.messenger.compose.view.theme.components.themedSurface
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import net.folivo.trixnity.core.model.RoomId

interface RoomListElementContainerView {
    @Composable
    fun LazyItemScope.create(
        roomId: RoomId,
        roomListViewModel: RoomListViewModel,
        roomListElementViewModel: RoomListElementViewModel,
        index: Int
    )
}

@Composable
fun LazyItemScope.RoomListElementContainer(
    roomId: RoomId,
    roomListViewModel: RoomListViewModel,
    roomListElementViewModel: RoomListElementViewModel,
    index: Int,
) {
    with(DI.get<RoomListElementContainerView>()) { create(roomId, roomListViewModel, roomListElementViewModel, index) }
}

class RoomListElementContainerViewImpl : RoomListElementContainerView {
    @Composable
    override fun LazyItemScope.create(
        roomId: RoomId,
        roomListViewModel: RoomListViewModel,
        roomListElementViewModel: RoomListElementViewModel,
        index: Int,
    ) {
        val selectedRoomId = roomListViewModel.selectedRoomId.collectAsState().value
        val roomName = roomListElementViewModel.roomName.collectAsState().value
        val isInvite = roomListElementViewModel.isInvite.collectAsState().value
        val interactionSource = remember { MutableInteractionSource() }
        val hasFocus = interactionSource.collectIsFocusedAsState().value
        val isKnock = roomListElementViewModel.isKnock.collectAsState().value == true
        val hoverable = roomName != null && isInvite != true && !isKnock
        val elementsSize = roomListViewModel.elements.collectAsState().value.size

        Box(
            Modifier.animateItem(
                fadeInSpec = null,
                fadeOutSpec = null,
                placementSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = IntOffset.VisibilityThreshold
                )
            )
                .then(
                    if (roomId == selectedRoomId) Modifier.themedSurface(
                        MaterialTheme.components.roomListSelection,
                        focused = hasFocus
                    )
                    else Modifier.themedSurface(MaterialTheme.components.roomListElement, focused = hasFocus)
                )
                .clickable(interactionSource, LocalIndication.current) {
                    if (hoverable) {
                        roomListViewModel.selectRoom(roomId)
                    }
                }
                .buttonPointerModifier(enabled = isInvite != true)
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (roomId == selectedRoomId) MaterialTheme.components.roomListSelection.contentColor else LocalContentColor.current
            ) {
                RoomListElement(roomListViewModel, roomListElementViewModel, index)
            }
        }
        if (index < elementsSize) {
            ThemedHorizontalDivider(
                style = MaterialTheme.components.roomListDivider
            )
        }
    }
}
