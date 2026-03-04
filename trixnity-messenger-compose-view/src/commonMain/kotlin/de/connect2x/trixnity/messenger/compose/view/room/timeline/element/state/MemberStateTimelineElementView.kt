package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.state

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.trixnity.messenger.compose.view.room.timeline.HorizontalDividerWithText
import de.connect2x.trixnity.messenger.compose.view.room.timeline.Indicator
import de.connect2x.trixnity.messenger.compose.view.room.timeline.IndicatorText
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.MemberStateTimelineElementViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass

interface MemberStateTimelineElementView : TimelineElementView<MemberStateTimelineElementViewModel>

class MemberStateTimelineElementViewImpl : MemberStateTimelineElementView {
    override val supports: KClass<MemberStateTimelineElementViewModel> = MemberStateTimelineElementViewModel::class

    override suspend fun waitFor(element: MemberStateTimelineElementViewModel) {
        element.changeMessage.filterNotNull().first()
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: MemberStateTimelineElementViewModel,
        index: Int,
    ) {
        StateElement(element)
        element.preJoinHistoryWarning.collectAsState().value?.let { 
            HorizontalDividerWithText(it) 
        }
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: MemberStateTimelineElementViewModel,
        index: Int,
    ) {
        StateElement(element)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: MemberStateTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            modifier = modifier,
            interactionSource = interactionSource,
            content = {
                StateElement(element)
            }
        )
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: MemberStateTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            modifier = modifier,
            interactionSource = interactionSource,
            content = {
                StateElement(element)
            }
        )
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: MemberStateTimelineElementViewModel
    ): ClipEntry? = null

    @Composable
    private fun StateElement(element: MemberStateTimelineElementViewModel) {
        val changeMessage = element.changeMessage.collectAsState().value
        changeMessage?.let {
            Indicator(MaterialTheme.colorScheme.tertiary, focusable = true) {
                IndicatorText(changeMessage, MaterialTheme.colorScheme.onTertiary)
            }
        }
    }
}
