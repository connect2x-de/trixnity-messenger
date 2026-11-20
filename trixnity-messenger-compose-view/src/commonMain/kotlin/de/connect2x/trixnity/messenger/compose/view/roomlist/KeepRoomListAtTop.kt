package de.connect2x.trixnity.messenger.compose.view.roomlist

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

@Composable
internal fun KeepRoomListAtTop(state: LazyListState, rooms: List<*>) {
    val wasAtTop =
        remember(state) {
                derivedStateOf { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 }
            }
            .value
    LaunchedEffect(rooms) { if (wasAtTop) state.requestScrollToItem(0) }
}
