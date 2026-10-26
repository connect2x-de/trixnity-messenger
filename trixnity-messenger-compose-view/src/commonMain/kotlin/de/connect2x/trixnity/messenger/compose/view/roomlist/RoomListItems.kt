package de.connect2x.trixnity.messenger.compose.view.roomlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.fastAny
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.trixnity.messenger.compose.view.roomlist.room.RoomListElementContainer
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel

@Composable
internal fun RoomListItems(
    roomListViewModel: RoomListViewModel,
    state: LazyListState,
    header: LazyListScope.() -> Unit = {},
    footer: LazyListScope.() -> Unit = {},
) {
    val allRooms = roomListViewModel.elements.collectAsState().value
    val selectedRoomId = roomListViewModel.selectedRoomId.collectAsState().value
    val roomKeys = remember(allRooms) { allRooms.map { it.roomId.full }.toSet() }
    val ignoredKeys by
        remember(state, roomKeys) {
            derivedStateOf { state.layoutInfo.visibleItemsInfo.map { it.key }.filter { it !in roomKeys } }
        }
    val focusedItem =
        remember(selectedRoomId, allRooms) {
            mutableStateOf(
                if (selectedRoomId != null && allRooms.fastAny { it.roomId == selectedRoomId }) {
                    selectedRoomId.full
                } else {
                    allRooms.firstOrNull()?.roomId?.full
                }
            )
        }
    LazyColumn(
        Modifier.fillMaxSize()
            .rovingFocusContainer(listState = state, focusedItem = focusedItem, ignoredKeys = ignoredKeys)
            .semantics { collectionInfo = CollectionInfo(rowCount = allRooms.size, columnCount = 0) },
        state,
    ) {
        header()
        itemsIndexed(allRooms, { _, element -> element.roomId.full }) { index, roomListElement ->
            Box(
                Modifier.rovingFocusItem(
                    isFocused = { focusedItem.value == roomListElement.roomId.full },
                    onFocus = { focusedItem.value = roomListElement.roomId.full },
                )
            ) {
                RoomListElementContainer(roomListElement.roomId, roomListViewModel, roomListElement, index)
            }
        }
        footer()
    }
}
