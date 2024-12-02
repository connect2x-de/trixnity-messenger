package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlin.reflect.KClass

// FIXME into DI
class TextRoomMessageTimelineElementView : TimelineElementView<RoomMessageTimelineElementViewModel.TextBased.Text> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.TextBased.Text> =
        RoomMessageTimelineElementViewModel.TextBased.Text::class

    @Composable
    override fun create(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.TextBased.Text,
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element)
    }
}
