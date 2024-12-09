package de.connect2x.messenger.compose.view.room.timeline.element.message.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

interface ElementDetailsViewSelector {
    @Composable
    fun create(element: TimelineElementViewModel<*>, onSave: () -> Unit, onClose: () -> Unit)
}

@Composable
fun ElementDetailsSelector(
    element: TimelineElementViewModel<*>,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    with(DI.get<ElementDetailsViewSelector>()) { create(element, onSave, onClose) }
}

class ElementDetailsViewSelectorImpl(val factories: List<ElementDetailsView<*>>) : ElementDetailsViewSelector {
    private val factoryMapping =
        MutableStateFlow<Map<KClass<out TimelineElementViewModel<*>>, ElementDetailsView<TimelineElementViewModel<*>>>>(
            emptyMap()
        )

    @Composable
    override fun create(element: TimelineElementViewModel<*>, onSave: () -> Unit, onClose: () -> Unit) {
        val timelineElementViewModelClass = element::class
        val factory = remember {
            factoryMapping.value[timelineElementViewModelClass]
                ?: run {
                    val foundFactory =
                        factories.firstOrNull {
                            it.supports.isInstance(element) && (
                                    element is RoomMessageTimelineElementViewModel.FileBased<*> &&
                                            it.supportedMimeTypes?.contains(element.mimeType) != false ||
                                            element !is RoomMessageTimelineElementViewModel.FileBased<*>
                                    )
                        }
                    if (foundFactory == null) return@run null
                    @Suppress("UNCHECKED_CAST")
                    foundFactory as ElementDetailsView<TimelineElementViewModel<*>>
                    factoryMapping.update { it + (timelineElementViewModelClass to foundFactory) }
                    foundFactory
                }
        }
        factory?.create(element, onSave, onClose)
            ?: run { // in case we show no overlay, we directly save
                log.warn { "no overlay found for ${element::class.qualifiedName} -> directly save" }
                onSave()
            }
    }

}
