package de.connect2x.messenger.compose.view.room.timeline.element.state

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.room.timeline.Indicator
import de.connect2x.messenger.compose.view.room.timeline.IndicatorText
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.MemberStateTimelineElementViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass


class MemberStateTimelineElementView : TimelineElementView<MemberStateTimelineElementViewModel> {
    override val supports: KClass<MemberStateTimelineElementViewModel> = MemberStateTimelineElementViewModel::class

    override suspend fun waitFor(element: MemberStateTimelineElementViewModel) {
        element.changeMessage.filterNotNull().first()
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: MemberStateTimelineElementViewModel,
    ) {
        StateElement(element)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: MemberStateTimelineElementViewModel,
    ) {
        StateElement(element)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: MemberStateTimelineElementViewModel
    ) {
        ReferencedMessagePill(
            holder = holder,
            content = {
                StateElement(element)
            }
        )
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: MemberStateTimelineElementViewModel
    ) {
        ReferencedMessagePill(
            holder = holder,
            content = {
                StateElement(element)
            }
        )
    }

    @Composable
    private fun StateElement(element: MemberStateTimelineElementViewModel) {
        val changeMessage = element.changeMessage.collectAsState().value
        changeMessage?.let {
            Indicator(MaterialTheme.colorScheme.tertiary) {
                IndicatorText(changeMessage, MaterialTheme.colorScheme.onTertiary)
            }
        }
    }
}
