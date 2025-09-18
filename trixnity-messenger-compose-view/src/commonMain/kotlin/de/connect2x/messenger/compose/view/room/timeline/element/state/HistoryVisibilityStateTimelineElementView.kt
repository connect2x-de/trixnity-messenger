package de.connect2x.messenger.compose.view.room.timeline.element.state

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.messenger.compose.view.room.timeline.Indicator
import de.connect2x.messenger.compose.view.room.timeline.IndicatorText
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.HistoryVisibilityStateTimelineElementViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass

interface HistoryVisibilityStateTimelineElementView :
    TimelineElementView<HistoryVisibilityStateTimelineElementViewModel>

class HistoryVisibilityStateTimelineElementViewImpl : HistoryVisibilityStateTimelineElementView {
    override val supports: KClass<HistoryVisibilityStateTimelineElementViewModel> =
        HistoryVisibilityStateTimelineElementViewModel::class

    override suspend fun waitFor(element: HistoryVisibilityStateTimelineElementViewModel) {
        element.changeMessage.filterNotNull().first()
    }

    // FIXME
    override fun isFocusable(): Boolean = false

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: HistoryVisibilityStateTimelineElementViewModel,
    ) {
        StateElement(element)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: HistoryVisibilityStateTimelineElementViewModel,
    ) {
        StateElement(element)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: HistoryVisibilityStateTimelineElementViewModel
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
        element: HistoryVisibilityStateTimelineElementViewModel
    ) {
        ReferencedMessagePill(
            holder = holder,
            content = {
                StateElement(element)
            }
        )
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: HistoryVisibilityStateTimelineElementViewModel
    ): ClipEntry? = null

    @Composable
    private fun StateElement(element: HistoryVisibilityStateTimelineElementViewModel) {
        val changeMessage = element.changeMessage.collectAsState().value
        changeMessage?.let {
            Indicator(MaterialTheme.colorScheme.tertiary) {
                IndicatorText(it, MaterialTheme.colorScheme.onTertiary)
            }
        }
    }
}
