package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass


private val log = KotlinLogging.logger {}

interface TimelineElementViewSelector :
    TimelineElementViewFactorySelector<TimelineElementView<TimelineElementViewModel<*>>> {
    suspend fun waitFor(element: TimelineElementViewModel<*>) =
        selectFactory(element).waitFor(element)

    fun isFocusable(element: TimelineElementViewModel<*>) =
        selectFactory(element).isFocusable()

    @Composable
    fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
    ) = rememberFactory(element).createInTimeline(holder, element)

    @Composable
    fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
    ) = rememberFactory(element).createAsPreview(holder, element)

    @Composable
    fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
        modifier: Modifier = Modifier,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    ) = rememberFactory(element).createReplyInTimeline(holder, element, modifier, interactionSource)

    @Composable
    fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
        modifier: Modifier = Modifier,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    ) = rememberFactory(element).createReplyInSendMessage(holder, element, modifier, interactionSource)

    @Composable
    fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
    ): ClipEntry? = rememberFactory(element).getClipEntry(holder, element)
}

@Composable
fun TimelineElementSelector(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    element: TimelineElementViewModel<*>,
) {
    with(DI.get<TimelineElementViewSelector>()) { createInTimeline(timelineElementHolderViewModel, element) }
}

class TimelineElementViewSelectorImpl(private val factories: List<TimelineElementView<*>>) :
    TimelineElementViewSelector {
    private val factoryMapping =
        MutableStateFlow<Map<KClass<out TimelineElementViewModel<*>>, TimelineElementView<TimelineElementViewModel<*>>>>(
            emptyMap()
        )

    override fun selectFactory(element: TimelineElementViewModel<*>): TimelineElementView<TimelineElementViewModel<*>> {
        val timelineElementViewModelClass = element::class
        return factoryMapping.value[timelineElementViewModelClass]
            ?: run {
                @Suppress("UNCHECKED_CAST")
                val foundFactory =
                    factories.firstOrNull { it.supports.isInstance(element) } as TimelineElementView<TimelineElementViewModel<*>>?
                        ?: run {
                            log.warn {
                                "There are no registered views for ${element::class.simpleName}. " +
                                        "This can be a missing view in the DI or might be an element that should not be " +
                                        "visible in the timeline."
                            }
                            EmptyTimelineElementView
                        }
                factoryMapping.update { it + (timelineElementViewModelClass to foundFactory) }
                foundFactory
            }
    }
}
