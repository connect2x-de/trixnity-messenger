package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        Box(
            Modifier
                .padding(start = 10.dp, end = 10.dp, top = 10.dp)
                .let {
                    if (repliedElementHolder.canScrollTo) {
                        it.clickable {
                            repliedElementHolder.scrollToElement()
                        }
                    } else it
                }
        ) {
            timelineElementViewSelector.createReplyInTimeline(repliedElementHolder, element)
        }
    }
}
