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
    suspend fun waitFor(element: TimelineElementViewModel<*>)

    @Composable
    fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
    )

    @Composable
    fun createAsPreview(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
    )

    @Composable
    fun createReplyInTimeline(element: TimelineElementViewModel<*>)

    @Composable
    fun createReplyInSendMessage(element: TimelineElementViewModel<*>)
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

    override suspend fun waitFor(element: TimelineElementViewModel<*>) {
        val factory = selectFactory(element)
        factory.waitFor(element)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
    ) {
        val factory = rememberSelectFactory(element)
        factory?.createInTimeline(holder, element)
    }

    @Composable
    override fun createAsPreview(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
    ) {
        val factory = rememberSelectFactory(element)
        factory?.createAsPreview(holder, element)
    }

    @Composable
    override fun createReplyInTimeline(
        element: TimelineElementViewModel<*>,
    ) {
        val factory = rememberSelectFactory(element)
        factory?.createReplyInTimeline(element)
    }

    @Composable
    override fun createReplyInSendMessage(
        element: TimelineElementViewModel<*>,
    ) {
        val factory = rememberSelectFactory(element)
        factory?.createReplyInSendMessage(element)
    }

    @Composable
    private fun rememberSelectFactory(element: TimelineElementViewModel<*>): TimelineElementView<TimelineElementViewModel<*>>? =
        remember(element) { selectFactory(element) }

    private fun selectFactory(element: TimelineElementViewModel<*>): TimelineElementView<TimelineElementViewModel<*>> {
        val timelineElementViewModelClass = element::class
        return factoryMapping.value[timelineElementViewModelClass]
            ?: run {
                val foundFactory = factories.firstOrNull { it.supports.isInstance(element) }
                    ?: run {
                        log.warn {
                            "There are no registered views for ${element::class.simpleName}. " +
                                    "This can be a missing view in the DI or might be an element that should not be " +
                                    "visible in the timeline."
                        }
                        EmptyTimelineElementView
                    }
                @Suppress("UNCHECKED_CAST")
                foundFactory as TimelineElementView<TimelineElementViewModel<*>>
                factoryMapping.update { it + (timelineElementViewModelClass to foundFactory) }
                foundFactory
            }
    }
}
