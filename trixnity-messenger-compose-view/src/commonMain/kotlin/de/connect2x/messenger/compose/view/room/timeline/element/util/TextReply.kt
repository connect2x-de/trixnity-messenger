package de.connect2x.messenger.compose.view.room.timeline.element.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel

@Composable
fun TextReplyInTimeline(element: RoomMessageTimelineElementViewModel.TextBased<*>) {
    TextReply(element, 4)
}

@Composable
fun TextReplyInSendMessage(element: RoomMessageTimelineElementViewModel.TextBased<*>) {
    TextReply(element, 2)
}

@Composable
private fun TextReply(element: RoomMessageTimelineElementViewModel.TextBased<*>, maxLines: Int) {
    Text(
        text = element.body,
        fontStyle = FontStyle.Italic,
        style = MaterialTheme.typography.bodySmall,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
