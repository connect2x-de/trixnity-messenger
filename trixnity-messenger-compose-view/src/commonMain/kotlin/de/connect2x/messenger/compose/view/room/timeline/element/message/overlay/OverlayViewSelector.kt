package de.connect2x.messenger.compose.view.room.timeline.element.message.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

interface OverlayViewSelector {
    @Composable
    fun createOverlay(element: TimelineElementViewModel<*>, onSave: () -> Unit, onClose: () -> Unit)
}

@Composable
fun OverlaySelector(
    element: TimelineElementViewModel<*>,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    with(DI.get<OverlayViewSelector>()) { createOverlay(element, onSave, onClose) }
}

class OverlayViewSelectorImpl(val factories: List<OverlayView<*>>) : OverlayViewSelector {
    private val factoryMapping =
        MutableStateFlow<Map<KClass<out TimelineElementViewModel<*>>, OverlayView<TimelineElementViewModel<*>>>>(
            emptyMap()
        )

    @Composable
    override fun createOverlay(element: TimelineElementViewModel<*>, onSave: () -> Unit, onClose: () -> Unit) {
        val timelineElementViewModelClass = element::class
        val factory = remember {
            factoryMapping.value[timelineElementViewModelClass]
                ?: run {
                    val foundFactory =
                        factories.firstOrNull {
                            it.supports.isInstance(timelineElementViewModelClass) && (
                                    element is RoomMessageTimelineElementViewModel.FileBased<*> &&
                                            it.supportedMimeTypes?.contains(element.mimeType) != false ||
                                            element !is RoomMessageTimelineElementViewModel.FileBased<*>
                                    )
                        }
                    if (foundFactory == null) return@run null
                    @Suppress("UNCHECKED_CAST")
                    foundFactory as OverlayView<TimelineElementViewModel<*>>
                    factoryMapping.update { it + (timelineElementViewModelClass to foundFactory) }
                    foundFactory
                }
        }
        factory?.create(element, onSave, onClose)
            ?: onSave() // in case we show no overlay, we directly save
    }

}
