package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

interface TimelineElementViewSelector {
    @Composable
    fun createInTimeline(holder: BaseTimelineElementHolderViewModel, element: TimelineElementViewModel<*>)

    @Composable
    fun createReplyInTimeline(element: TimelineElementViewModel<*>)

    @Composable
    fun createReplyInSendMessage(element: TimelineElementViewModel<*>)
}

@Composable
fun TimelineElementSelector(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    element: TimelineElementViewModel<*>
) {
    with(DI.get<TimelineElementViewSelector>()) { createInTimeline(timelineElementHolderViewModel, element) }
}

class TimelineElementViewSelectorImpl(private val factories: List<TimelineElementView<*>>) :
    TimelineElementViewSelector {
    private val factoryMapping =
        MutableStateFlow<Map<KClass<out TimelineElementViewModel<*>>, TimelineElementView<TimelineElementViewModel<*>>>>(
            emptyMap()
        )

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>
    ) {
        val factory = selectFactory(element)
        factory?.createInTimeline(holder, element) ?: warn(element)
    }

    @Composable
    override fun createReplyInTimeline(
        element: TimelineElementViewModel<*>
    ) {
        val factory = selectFactory(element)
        factory?.createReplyInTimeline(element) ?: warn(element)
    }

    @Composable
    override fun createReplyInSendMessage(element: TimelineElementViewModel<*>) {
        val factory = selectFactory(element)
        factory?.createReplyInSendMessage(element) ?: warn(element)
    }

    @Composable
    private fun selectFactory(element: TimelineElementViewModel<*>): TimelineElementView<TimelineElementViewModel<*>>? {
        val timelineElementViewModelClass = element::class
        val factory = remember(element) {
            factoryMapping.value[timelineElementViewModelClass]
                ?: run {
                    val foundFactory =
                        factories.firstOrNull { it.supports.isInstance(element) }
                    if (foundFactory == null) return@run null
                    @Suppress("UNCHECKED_CAST")
                    foundFactory as TimelineElementView<TimelineElementViewModel<*>>
                    factoryMapping.update { it + (timelineElementViewModelClass to foundFactory) }
                    foundFactory
                }
        }
        return factory
    }

    private fun warn(element: TimelineElementViewModel<*>) {
        log.warn {
            "There are no registered views for ${element::class.simpleName}. " +
                    "This can be a missing view in the DI or might be an element that should not be " +
                    "visible in the timeline."
        }
    }
}
