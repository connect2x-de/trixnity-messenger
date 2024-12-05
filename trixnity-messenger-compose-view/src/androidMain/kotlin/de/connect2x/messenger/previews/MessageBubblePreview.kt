package de.connect2x.messenger.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.room.timeline.element.message.TextBasedRoomMessageTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.messenger.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewTimelineElementViewModel1
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlinx.coroutines.flow.StateFlow

@Preview
@Composable
fun MessageBubblePreview() {
    val element = object : RoomMessageTimelineElementViewModel.TextBased.Text {
        override val body: String = "Hello everyone!"
        override val formattedBody: String = "Hello <b/>everyone!"
        override val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
        override val mentionsInFormattedBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
        override fun openMention(timelineElementMention: TimelineElementMention) {}
    }
    InitMessengerPreview {
        MessageBubble(
            holder = PreviewTimelineElementViewModel1(),
            element = element,
            showDate = true,
            needsMaxWidth = false,
        ) {
            TextBasedRoomMessageTimelineElementView(
                PreviewTimelineElementViewModel1(),
                element,
            )
        }
    }
}
