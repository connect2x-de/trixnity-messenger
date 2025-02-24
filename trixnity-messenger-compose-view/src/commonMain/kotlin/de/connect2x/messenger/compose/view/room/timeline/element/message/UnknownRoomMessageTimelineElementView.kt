package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.Unknown
import kotlin.reflect.KClass


class UnknownRoomMessageTimelineElementView : TimelineElementView<Unknown> {
    override val supports: KClass<Unknown> =
        Unknown::class

    override suspend fun waitFor(element: Unknown) {
        // NO-OP (has default size)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: Unknown,
    ) {
        UnknownMessageElement(element)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: Unknown,
    ) {
        UnknownMessageElement(element)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: Unknown,
    ) {
        ReferencedMessagePill(
            holder = holder,
            content = {
                Text(
                    text = element.fallbackBody,
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        )
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: Unknown,
    ) {
        ReferencedMessagePill(
            holder = holder,
            content = {
                Text(
                    text = element.fallbackBody,
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        )
    }

}

@Composable
internal fun UnknownMessageElement(element: Unknown) {
    Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp)) {
        Text(element.fallbackBody, style = MaterialTheme.typography.bodyMedium)
    }
}
