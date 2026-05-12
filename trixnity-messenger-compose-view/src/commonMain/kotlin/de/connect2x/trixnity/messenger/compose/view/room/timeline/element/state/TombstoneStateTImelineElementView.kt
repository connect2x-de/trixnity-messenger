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
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.TombstoneStateTimelineElementViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass

interface TombstoneStateTimelineElementView : TimelineElementView<TombstoneStateTimelineElementViewModel>


class TombstoneStateTimelineElementViewImpl : TombstoneStateTimelineElementView {
    override val supports: KClass<TombstoneStateTimelineElementViewModel> =
        TombstoneStateTimelineElementViewModel::class

    override suspend fun waitFor(element: TombstoneStateTimelineElementViewModel) {
        element.changeMessage.filterNotNull().first()
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: TombstoneStateTimelineElementViewModel,
        index: Int,
    ) {
        StateElement(element)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: TombstoneStateTimelineElementViewModel,
        index: Int,
    ) {
        StateElement(element)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: TombstoneStateTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource
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
        element: TombstoneStateTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource
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
        element: TombstoneStateTimelineElementViewModel
    ): ClipEntry? = null
}

@Composable
private fun StateElement(element: TombstoneStateTimelineElementViewModel) {
    val changeMessage = element.changeMessage.collectAsState().value

    Indicator(MaterialTheme.colorScheme.tertiary, focusable = true) {
        IndicatorText(changeMessage, MaterialTheme.colorScheme.onTertiary)
    }
}


