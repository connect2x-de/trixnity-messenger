package de.connect2x.messenger.compose.view.room.timeline.element.state

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.room.timeline.Indicator
import de.connect2x.messenger.compose.view.room.timeline.IndicatorText
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.CanonicalAliasStateTimelineElementViewModel
import kotlin.reflect.KClass

class CanonicalAliasStateTimelineElementView : TimelineElementView<CanonicalAliasStateTimelineElementViewModel> {
    override val supports: KClass<CanonicalAliasStateTimelineElementViewModel> =
        CanonicalAliasStateTimelineElementViewModel::class

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: CanonicalAliasStateTimelineElementViewModel
    ) {
        val changeMessage = element.changeMessage.collectAsState().value
        changeMessage?.forEach {
            Indicator(MaterialTheme.colorScheme.tertiary) {
                IndicatorText(it, MaterialTheme.colorScheme.onTertiary)
            }
        }
    }

}
