package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel

@Composable
fun shortenFileName(element: RoomMessageTimelineElementViewModel.FileBased<*>): String {
    return remember(element.name) {
        element.name.let { name ->
            if (name.length > 20) {
                "${name.take(13)}...${name.takeLast(4)}"
            } else name
        }
    }
}
