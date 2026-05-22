package de.connect2x.trixnity.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private val log: Logger =
    Logger("de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementViewSelectorKt")

interface TimelineElementViewSelector :
    TimelineElementViewFactorySelector<TimelineElementView<TimelineElementViewModel<*>>> {
    suspend fun waitFor(element: TimelineElementViewModel<*>) = selectFactory(element).waitFor(element)

    fun isFocusable(element: TimelineElementViewModel<*>) = selectFactory(element).isFocusable()

    @Composable
    fun createInTimeline(holder: BaseTimelineElementHolderViewModel, element: TimelineElementViewModel<*>, index: Int) =
        rememberFactory(element).createInTimeline(holder, element, index)

    @Composable
    fun createAsPreview(holder: TimelineElementHolderViewModel, element: TimelineElementViewModel<*>, index: Int) =
        rememberFactory(element).createAsPreview(holder, element, index)

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
    fun getClipEntry(holder: BaseTimelineElementHolderViewModel, element: TimelineElementViewModel<*>): ClipEntry? =
        rememberFactory(element).getClipEntry(holder, element)

    /**
     * Optional label for accessibility. This is read to a user when the timeline element is focused in the timeline.
     */
    fun a11yLabel(element: TimelineElementViewModel<*>, i18n: I18nView): String? =
        selectFactory(element).a11yLabel(element, i18n)
}

@Composable
fun TimelineElementSelector(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    element: TimelineElementViewModel<*>,
    index: Int,
) {
    with(DI.get<TimelineElementViewSelector>()) { createInTimeline(timelineElementHolderViewModel, element, index) }
}

class TimelineElementViewSelectorImpl(private val factories: List<TimelineElementView<*>>) :
    TimelineElementViewSelector {
    private val factoryMapping =
        MutableStateFlow<
            Map<KClass<out TimelineElementViewModel<*>>, TimelineElementView<TimelineElementViewModel<*>>>
        >(
            emptyMap()
        )

    override fun selectFactory(element: TimelineElementViewModel<*>): TimelineElementView<TimelineElementViewModel<*>> {
        val timelineElementViewModelClass = element::class
        return factoryMapping.value[timelineElementViewModelClass]
            ?: run {
                @Suppress("UNCHECKED_CAST")
                val foundFactory =
                    factories.firstOrNull { it.supports.isInstance(element) }
                        as TimelineElementView<TimelineElementViewModel<*>>?
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
