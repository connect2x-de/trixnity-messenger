package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.details

import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementViewFactorySelector
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*

private val log = KotlinLogging.logger { }

interface ElementDetailsViewSelector :
    TimelineElementViewFactorySelector<TimelineElementDetailsView<TimelineElementViewModel<*>>?>

class ElementDetailsViewSelectorImpl(
    private val factories: List<TimelineElementDetailsView<*>>,
) : ElementDetailsViewSelector {
    override fun selectFactory(element: TimelineElementViewModel<*>): TimelineElementDetailsView<TimelineElementViewModel<*>>? {
        val mimeTypeString = (element as? RoomMessageTimelineElementViewModel.FileBased<*>)?.mimeType
        val foundFactory = mimeTypeString?.let {
            try {
                val mimeType = ContentType.parse(it)
                factories.firstOrNull {
                    it.supports.isInstance(element) && (it.supportsMimeType(mimeType))
                }.also {
                    log.trace { "Found factory $it to support details view of $element" }
                }
            } catch (e: Exception) {
                log.error { "Got exception $e when trying to parse mimeType $mimeTypeString of file $element" }
                null
            }
        }
        @Suppress("UNCHECKED_CAST")
        return when {
            foundFactory != null -> foundFactory as TimelineElementDetailsView<TimelineElementViewModel<*>>
            element is RoomMessageTimelineElementViewModel.FileBased -> PreviewNotSupportedTimelineElementDetailsView() as TimelineElementDetailsView<TimelineElementViewModel<*>>
            else -> null

        }
    }
}
