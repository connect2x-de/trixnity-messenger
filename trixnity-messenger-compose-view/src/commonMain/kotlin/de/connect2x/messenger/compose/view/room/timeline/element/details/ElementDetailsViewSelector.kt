package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging


private val log = KotlinLogging.logger {}

interface ElementDetailsViewSelector {
    @Composable
    fun create(
        element: TimelineElementViewModel<*>,
        onSave: () -> Unit,
        onClose: () -> Unit,
    )
}

@Composable
fun ElementDetailsSelector(
    element: TimelineElementViewModel<*>,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    with(DI.get<ElementDetailsViewSelector>()) { create(element, onSave, onClose) }
}

class ElementDetailsViewSelectorImpl(
    private val factories: List<TimelineElementDetailsView<*>>,
) : ElementDetailsViewSelector {
    @Composable
    override fun create(element: TimelineElementViewModel<*>, onSave: () -> Unit, onClose: () -> Unit) {
        val factory = remember {
            val mimeType = (element as? RoomMessageTimelineElementViewModel.FileBased<*>)?.mimeType
            val foundFactory =
                factories.firstOrNull {
                    it.supports.isInstance(element) && it.supportedMimeTypes.contains(mimeType)
                }
            if (foundFactory == null) null
            else {
                @Suppress("UNCHECKED_CAST")
                foundFactory as TimelineElementDetailsView<TimelineElementViewModel<*>>
            }
        }
        factory?.create(element, onSave, onClose)
            ?: run { // in case we show no overlay, we directly save
                log.warn { "no overlay found for ${element::class.simpleName} -> directly save" }
                onSave()
            }
    }
}
