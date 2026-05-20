package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel

@Composable
fun RepliedElement(holder: BaseTimelineElementHolderViewModel) {
    val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
    val repliedElementHolder = holder.repliedElement.collectAsState().value
    val element = repliedElementHolder?.element?.collectAsState()?.value
    val interactionSource = remember { MutableInteractionSource() }

    if (repliedElementHolder != null && element != null) {
        Box(Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp)) {
            timelineElementViewSelector.createReplyInTimeline(
                holder = repliedElementHolder,
                element = element,
                modifier =
                    Modifier.buttonPointerModifier().clickable(interactionSource, LocalIndication.current) {
                        repliedElementHolder.jumpTo()
                    },
                interactionSource = interactionSource,
            )
        }
    }
}
