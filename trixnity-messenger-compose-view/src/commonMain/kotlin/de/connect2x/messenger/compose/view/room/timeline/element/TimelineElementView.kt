package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import kotlin.reflect.KClass

interface TimelineElementView<V : TimelineElementViewModel<*>> {
    @Composable
    fun create(holder: BaseTimelineElementHolderViewModel, element: V)

    val supports: KClass<V>
}
