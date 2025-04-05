package de.connect2x.messenger.compose.view.room.timeline.element.details

import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewFactorySelector
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel

interface ElementDetailsViewSelector :
    TimelineElementViewFactorySelector<TimelineElementDetailsView<TimelineElementViewModel<*>>?>

class ElementDetailsViewSelectorImpl(
    private val factories: List<TimelineElementDetailsView<*>>,
) : ElementDetailsViewSelector {
    override fun selectFactory(element: TimelineElementViewModel<*>): TimelineElementDetailsView<TimelineElementViewModel<*>>? {
        val mimeType = (element as? RoomMessageTimelineElementViewModel.FileBased<*>)?.mimeType
        val foundFactory = mimeType?.let {
            factories.firstOrNull {
                it.supports.isInstance(element) && (it.supportsMimeType(mimeType))
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
