package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.state

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.trixnity.messenger.compose.view.room.timeline.Indicator
import de.connect2x.trixnity.messenger.compose.view.room.timeline.IndicatorText
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.TopicStateTimelineElementViewModel
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

interface TopicStateTimelineElementView : TimelineElementView<TopicStateTimelineElementViewModel>

class TopicStateTimelineElementViewImpl : TopicStateTimelineElementView {
    override val supports: KClass<TopicStateTimelineElementViewModel> = TopicStateTimelineElementViewModel::class

    override suspend fun waitFor(element: TopicStateTimelineElementViewModel) {
        element.changeMessage.filterNotNull().first()
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: TopicStateTimelineElementViewModel,
        index: Int,
    ) {
        StateElement(element)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: TopicStateTimelineElementViewModel,
        index: Int,
    ) {
        StateElement(element)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: TopicStateTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            modifier = modifier,
            interactionSource = interactionSource,
            content = { StateElement(element) },
        )
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: TopicStateTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            modifier = modifier,
            interactionSource = interactionSource,
            content = { StateElement(element) },
        )
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: TopicStateTimelineElementViewModel,
    ): ClipEntry? = null

    @Composable
    private fun StateElement(element: TopicStateTimelineElementViewModel) {
        val changeMessage = element.changeMessage.collectAsState().value
        changeMessage?.let {
            Indicator(MaterialTheme.colorScheme.tertiary, focusable = true) {
                IndicatorText(it, MaterialTheme.colorScheme.onTertiary)
            }
        }
    }
}
