package de.connect2x.messenger.compose.view.room.timeline.element.state

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.room.timeline.Indicator
import de.connect2x.messenger.compose.view.room.timeline.IndicatorText
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.CreateStateTimelineElementViewModel
import kotlin.reflect.KClass

class CreateStateTimelineElementView: TimelineElementView<CreateStateTimelineElementViewModel> {
    override val supports: KClass<CreateStateTimelineElementViewModel> = CreateStateTimelineElementViewModel::class

    @Composable
    override fun create(
        holder: BaseTimelineElementHolderViewModel,
        element: CreateStateTimelineElementViewModel
    ) {
        val message = element.message.collectAsState().value
        message?.let {
            Indicator(MaterialTheme.colorScheme.tertiary) {
                IndicatorText(it, MaterialTheme.colorScheme.onTertiary)
            }
        }
    }
}
