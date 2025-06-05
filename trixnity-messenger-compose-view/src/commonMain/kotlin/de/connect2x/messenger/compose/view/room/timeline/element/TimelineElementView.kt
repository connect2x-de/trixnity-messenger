package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import kotlin.reflect.KClass

interface TimelineElementView<V : TimelineElementViewModel<*>> {
    val supports: KClass<out V>

    suspend fun waitFor(element: V)

    @Composable
    fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: V,
    )

    @Composable
    fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: V,
    )

    @Composable
    fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: V,
    )

    @Composable
    fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: V,
    )

    @Composable
    fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: V,
    ): ClipEntry?
}

object EmptyTimelineElementView : TimelineElementView<TimelineElementViewModel<*>> {
    override val supports: KClass<TimelineElementViewModel.Empty>
        get() = TimelineElementViewModel.Empty::class


    override suspend fun waitFor(element: TimelineElementViewModel<*>) {}

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
    ) {
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
    ) {
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>
    ) {
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>
    ): ClipEntry? = null

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>
    ) {
    }
}
