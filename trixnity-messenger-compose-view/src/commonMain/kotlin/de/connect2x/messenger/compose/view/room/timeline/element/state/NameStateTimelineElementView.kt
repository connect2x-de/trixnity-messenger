package de.connect2x.messenger.compose.view.room.timeline.element.state

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.room.timeline.Indicator
import de.connect2x.messenger.compose.view.room.timeline.IndicatorText
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.NameStateTimelineElementViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass


class NameStateTimelineElementView : TimelineElementView<NameStateTimelineElementViewModel> {
    override val supports: KClass<NameStateTimelineElementViewModel> = NameStateTimelineElementViewModel::class

    override suspend fun waitFor(element: NameStateTimelineElementViewModel) {
        element.changeMessage.filterNotNull().first()
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: NameStateTimelineElementViewModel
    ) {
        val changeMessage = element.changeMessage.collectAsState().value
        changeMessage?.let {
            Indicator(MaterialTheme.colorScheme.tertiary) {
                IndicatorText(it, MaterialTheme.colorScheme.onTertiary)
            }
        }
    }

    @Composable
    override fun createAsMessagePreview(
        holder: BaseTimelineElementHolderViewModel,
        element: NameStateTimelineElementViewModel,
    ) {
        // NO-OP
    }
}
