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
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubbleDisplayConfig
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlin.reflect.KClass


class UnknownRoomMessageTimelineElementView : TimelineElementView<RoomMessageTimelineElementViewModel.Unknown> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.Unknown> =
        RoomMessageTimelineElementViewModel.Unknown::class

    override suspend fun waitFor(element: RoomMessageTimelineElementViewModel.Unknown) {
        // NO-OP (has default size)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.Unknown,
    ) {
        UnknownMessageElement(element)
    }

    @Composable
    override fun createAsMessagePreview(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.Unknown,
        config: MessageBubbleDisplayConfig.() -> Unit,
    ) {
        UnknownMessageElement(element)
    }

    @Composable
    override fun createReplyInTimeline(element: RoomMessageTimelineElementViewModel.Unknown) {
        Text(
            text = element.fallbackBody,
            fontStyle = FontStyle.Italic,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }

    @Composable
    override fun createReplyInSendMessage(element: RoomMessageTimelineElementViewModel.Unknown) {
        Text(
            text = element.fallbackBody,
            fontStyle = FontStyle.Italic,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }

}

@Composable
internal fun UnknownMessageElement(element: RoomMessageTimelineElementViewModel.Unknown) {
    Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp)) {
        Text(element.fallbackBody, style = MaterialTheme.typography.bodyMedium)
    }
}
