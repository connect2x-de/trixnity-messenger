package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import kotlin.reflect.KClass


interface TimelineElementView<V : TimelineElementViewModel<*>> {
    val supports: KClass<V>

    suspend fun waitFor(element: V)

    @Composable
    fun createInTimeline(holder: BaseTimelineElementHolderViewModel, element: V)

    @Composable
    fun createAsMessagePreview(
        holder: BaseTimelineElementHolderViewModel,
        element: V,
    ) {
    }

    @Composable
    fun createReplyInTimeline(element: V) {
    }

    @Composable
    fun createReplyInSendMessage(element: V) {
    }
}

object EmptyTimelineElementView : TimelineElementView<TimelineElementViewModel.Empty> {
    override val supports: KClass<TimelineElementViewModel.Empty>
        get() = TimelineElementViewModel.Empty::class

    override suspend fun waitFor(element: TimelineElementViewModel.Empty) {}

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel.Empty,
    ) {
    }
}
