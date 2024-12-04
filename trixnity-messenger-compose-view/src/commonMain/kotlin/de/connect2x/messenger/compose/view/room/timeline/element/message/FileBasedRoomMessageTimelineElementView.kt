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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.messenger.compose.view.room.timeline.element.util.OverflowingFileInfo
import de.connect2x.messenger.compose.view.room.timeline.element.util.asOutboxElementHolder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize

@Composable
fun FileBasedRoomMessageTimelineElementView(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.FileBased<*>,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.(onSave: () -> Unit) -> Unit,
) {
    val i18n = DI.current.get<I18nView>()

    val error = element.downloadMediaError.collectAsState().value
    val saveDialogOpen = remember { mutableStateOf(false) }
    if (saveDialogOpen.value) SaveFileDialog(
        element.name,
        element.mimeType,
        error,
        element::downloadMedia,
        { saveDialogOpen.value = false },
    )

    MessageBubble(
        holder,
        element,
        showDate = true,
        needsMaxWidth = true, // FIXME ?
        additionalContextActions = { onClose ->
            // name
            val nameAndSize = "${element.name}:" + (element.size?.let { " " + formatSize(it.toLong()) } ?: "")
            Tooltip(
                { TooltipText(nameAndSize) }
            ) {
                OverflowingFileInfo(
                    nameAndSize,
                    modifier = Modifier.padding(5.dp),
                )
            }
            HorizontalDivider()
            // download action
            BaseTimelineElementHolderContextMenuAction(
                label = i18n.downloadMessage(),
                action = { saveDialogOpen.value = true },
            ).render(onClose)

        },
        overlay,
    ) { showActionMenu ->
        FileBasedView(holder, element, saveDialogOpen, showActionMenu, content)
    }

}

@Composable
internal fun FileBasedView(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.FileBased<*>,
    saveDialogOpen: MutableState<Boolean>,
    showActionMenu: () -> Unit,
    content: @Composable ColumnScope.(onSave: () -> Unit) -> Unit
) {
    val downloadProgressElement = element.downloadMediaProgress.collectAsState()
    val uploadProgress = holder.asOutboxElementHolder()?.uploadProgress?.collectAsState()?.value

    Box(
        Modifier
            .padding(10.dp)
    ) {
        Column(
            Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { element.open() },
                        onLongPress = { showActionMenu() },
                    )
                }
                .padding(10.dp)
                .buttonPointerModifier()
        ) {
            // content based on the actual file
            content {
                saveDialogOpen.value = true
            }
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

}
