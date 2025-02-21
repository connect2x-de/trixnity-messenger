package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel

@Composable
fun RepliedElement(holder: BaseTimelineElementHolderViewModel) {
    val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
    val repliedElementHolder = holder.repliedElement.collectAsState().value
    val element = repliedElementHolder?.element?.collectAsState()?.value

    if (repliedElementHolder != null && element != null) {
        timelineElementViewSelector.createReplyInTimeline(repliedElementHolder, element)
    }
}
