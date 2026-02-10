package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedActionMenu
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedActionMenuItem
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedHorizontalDivider
import de.connect2x.trixnity.messenger.compose.view.theme.components.themedSurface
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel

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
        val showActionMenu = remember { mutableStateOf(false) }
        val i18n = DI.current.get<I18nView>()
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
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                showActionMenu.value = true
                            }
                        }
                    }
                }.clickable(interactionSource, LocalIndication.current) {
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
                if (hoverable) {
                    Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        ThemedActionMenu(
                            interactionSource,
                            showActionMenu,
                            listOf(
                                ThemedActionMenuItem(
                                    i18n.markRoomAsUnread(),
                                    action = { roomListElementViewModel.markUnread() })
                            ),
                            additionalContextActions = {},
                            openActionMenuIcon = {
                                Icon(Icons.Default.MoreHoriz, null, tint = Color.White)
                            }
                        )
                    }
                }
            }
        }
        if (index < elementsSize) {
            ThemedHorizontalDivider(
                style = MaterialTheme.components.roomListDivider
            )
        }
    }
}
