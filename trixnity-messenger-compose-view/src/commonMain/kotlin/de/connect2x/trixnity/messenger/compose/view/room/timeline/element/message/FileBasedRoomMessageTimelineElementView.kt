package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.details.ElementDetailsViewSelector
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.file.FileBasedTimelineElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel

interface FileBasedRoomMessageTimelineElementView {
    @Composable
    fun create(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.FileBased<*>,
        isPreview: Boolean,
        displayProgressOverElement: Boolean,
        index: Int,
        overlay: (@Composable BoxScope.() -> Unit)?,
        content: @Composable ColumnScope.(showActionMenu: () -> Unit, onSave: () -> Unit) -> Unit,
    )
}

@Composable
fun FileBasedRoomMessageTimelineElement(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.FileBased<*>,
    isPreview: Boolean = false,
    displayProgressOverElement: Boolean = false,
    index: Int,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.(showActionMenu: () -> Unit, onSave: () -> Unit) -> Unit,
) {
    DI.get<FileBasedRoomMessageTimelineElementView>()
        .create(holder, element, isPreview, displayProgressOverElement, index, overlay, content)
}

class FileBasedRoomMessageTimelineElementViewImpl : FileBasedRoomMessageTimelineElementView {
    @Composable
    override fun create(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.FileBased<*>,
        isPreview: Boolean,
        displayProgressOverElement: Boolean,
        index: Int,
        overlay: (@Composable BoxScope.() -> Unit)?,
        content: @Composable ColumnScope.(showActionMenu: () -> Unit, onSave: () -> Unit) -> Unit,
    ) {
        FileBasedTimelineElement(
            holder = holder,
            element = element,
            isPreview = isPreview,
            displayProgressOverElement = displayProgressOverElement,
            index = index,
            overlay = overlay,
            content = content,
            caption = {
                if (element.hasCaption && element.body.isNotBlank()) {
                    TextRoomMessageTimelineElementView(holder, element) {}
                }
            },
            details = { onClose ->
                val elementDetailsFactory = DI.get<ElementDetailsViewSelector>().rememberFactory(element)
                elementDetailsFactory?.create(
                    element = element,
                    onSave = { element.showSaveDialog() },
                    onClose = onClose,
                )
            },
        )
    }
}
