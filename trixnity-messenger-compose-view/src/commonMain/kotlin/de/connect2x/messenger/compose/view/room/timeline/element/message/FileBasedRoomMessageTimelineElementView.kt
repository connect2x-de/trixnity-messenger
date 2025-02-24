package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.DownloadProgress
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.files.SaveFileDialog
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.details.ElementDetailsSelector
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.messenger.compose.view.room.timeline.element.util.asOutboxElementHolder
import de.connect2x.messenger.compose.view.room.timeline.element.util.shortenFileName
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel

interface FileBasedRoomMessageTimelineElementView {
    @Composable
    fun create(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.FileBased<*>,
        overlay: @Composable BoxScope.() -> Unit,
        content: @Composable ColumnScope.(showActionMenu: () -> Unit, onSave: () -> Unit) -> Unit,
    )
}

@Composable
fun FileBasedRoomMessageTimelineElement(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.FileBased<*>,
    overlay: @Composable BoxScope.() -> Unit,
    content: @Composable ColumnScope.(showActionMenu: () -> Unit, onSave: () -> Unit) -> Unit,
) {
    DI.get<FileBasedRoomMessageTimelineElementView>()
        .create(holder, element, overlay, content)
}

class FileBasedRoomMessageTimelineElementViewImpl : FileBasedRoomMessageTimelineElementView {
    @Composable
    override fun create(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.FileBased<*>,
        overlay: @Composable BoxScope.() -> Unit,
        content: @Composable ColumnScope.(showActionMenu: () -> Unit, onSave: () -> Unit) -> Unit,
    ) {
        val error = element.downloadMediaError.collectAsState().value
        var saveDialogOpen by remember { mutableStateOf(false) }
        if (saveDialogOpen) SaveFileDialog(
            element.name,
            element.mimeType,
            error,
            element::downloadMedia,
        ) { saveDialogOpen = false }

        FileBasedRoomMessageTimelineElementMessageBubble(holder, element, { saveDialogOpen = true }, overlay, content)
    }
}

@Composable
fun FileBasedRoomMessageTimelineElementMessageBubble(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.FileBased<*>,
    onSave: () -> Unit,
    overlay: @Composable BoxScope.() -> Unit,
    content: @Composable ColumnScope.(() -> Unit, () -> Unit) -> Unit
) {
    val i18n = DI.current.get<I18nView>()
    MessageBubble(
        holder,
        needsMaxWidth = true,
        additionalContextActions = { onClose ->
            // name
            Tooltip(
                { TooltipText("${element.name} " + (element.size ?: "")) } // full name
            ) {
                Text(
                    "${shortenFileName(element)} ${element.size ?: ""}", // shortened name
                    modifier = Modifier.padding(5.dp),
                    maxLines = 1,
                )
            }
            HorizontalDivider()
            // download action
            BaseTimelineElementHolderContextMenuAction(
                label = i18n.downloadMessage(),
                action = onSave,
            ).render(onClose)
        },
        overlay,
    ) { showActionMenu ->
        FileBasedView(holder, element, onSave, showActionMenu, content)
    }
}

@Composable
internal fun FileBasedView(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.FileBased<*>,
    onSave: () -> Unit,
    showActionMenu: () -> Unit,
    content: @Composable ColumnScope.(onShowActionMenu: () -> Unit, openElementDetails: () -> Unit) -> Unit
) {
    val downloadProgressElement = element.downloadMediaProgress.collectAsState()
    val uploadProgress = holder.asOutboxElementHolder()?.uploadProgress?.collectAsState()?.value

    var openElementDetails by remember { mutableStateOf(false) }

    Column(
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { openElementDetails = true },
                    onLongPress = { showActionMenu() },
                )
            }
            .buttonPointerModifier()
    ) {
        // content based on the actual file
        content(showActionMenu) {
            openElementDetails = true
        }
    }

    if (uploadProgress != null) {
        Box {
            DownloadProgress(uploadProgress, cancel = {
                if (holder is OutboxElementHolderViewModel) {
                    holder.abortSend()
                } else {
                    Unit
                }
            })
        }
    }
    downloadProgressElement.value?.let {
        Spacer(Modifier.size(10.dp))
        Box(Modifier.height(40.dp)) {
            DownloadProgress(
                it,
                { element.cancelDownloadMedia() },
                Color.DarkGray
            )
        }
        Spacer(Modifier.size(10.dp))
    }

    if (openElementDetails) {
        ElementDetailsSelector(element, onSave) {
            openElementDetails = false
        }
    }
}
