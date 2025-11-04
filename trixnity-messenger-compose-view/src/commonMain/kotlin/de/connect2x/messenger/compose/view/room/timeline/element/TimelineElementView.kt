package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import kotlin.reflect.KClass

interface TimelineElementView<V : TimelineElementViewModel<*>> {
    val supports: KClass<out V>

    suspend fun waitFor(element: V)

    fun isFocusable(): Boolean

    @Composable
    fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: V,
        index: Int,
    )

    @Composable
    fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: V,
        index: Int,
    )

    @Composable
    fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: V,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    )

    @Composable
    fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: V,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
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

    override fun isFocusable(): Boolean = false

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
        index: Int,
    ) {
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
        index: Int,
    ) {
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
    ): ClipEntry? = null

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
    }
}
