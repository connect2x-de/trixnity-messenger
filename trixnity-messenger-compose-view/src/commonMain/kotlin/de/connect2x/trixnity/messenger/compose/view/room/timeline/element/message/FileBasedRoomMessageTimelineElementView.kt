package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.common.DownloadProgress
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.files.SaveFileDialog
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.pointerMoveFilter
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.details.ElementDetailsViewSelector
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.FileContentOverlay
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.util.asOutboxElementHolder
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.util.shortenFileName
import de.connect2x.trixnity.messenger.compose.view.util.ifNotNull
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
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
        val error = element.downloadMediaError.collectAsState().value
        var saveDialogOpen by remember { mutableStateOf(false) }
        if (saveDialogOpen) SaveFileDialog(
            element.name,
            element.mimeType,
            error,
            element::downloadMedia,
        ) { saveDialogOpen = false }
        FileBasedRoomMessageTimelineElementMessageBubble(
            holder,
            element,
            { saveDialogOpen = true },
            isPreview,
            displayProgressOverElement,
            index,
            overlay,
            content
        )
    }
}

@Composable
fun FileBasedRoomMessageTimelineElementMessageBubble(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.FileBased<*>,
    onSave: () -> Unit,
    isPreview: Boolean = false,
    displayProgressOverElement: Boolean,
    index: Int,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.(() -> Unit, () -> Unit) -> Unit
) {
    val i18n = DI.get<I18nView>()
    val configuration = DI.get<MatrixMessengerConfiguration>()

    MessageBubble(
        holder,
        needsMaxWidth = true,
        additionalContextActions = { onClose ->
            // name
            Tooltip(
                { Text("${element.name}${element.size.ifNotNull { " $it" }}") } // full name
            ) {
                Text(
                    "${shortenFileName(element)}${element.size.ifNotNull { " $it" }}", // shortened name
                    modifier = Modifier.padding(5.dp),
                    maxLines = 1,
                )
            }
            HorizontalDivider()
            // download action
            BaseTimelineElementHolderContextMenuAction(
                label = i18n.downloadMessage(),
                isEnabled = !configuration.downloadsDisabled,
                action = onSave,
            ).render(onClose)
        },
        isPreview = isPreview,
        index = index,
    ) { showActionMenu ->
        Column {
            FileBasedView(
                holder,
                element,
                onSave,
                showActionMenu,
                displayProgressOverElement,
                isPreview,
                overlay,
                content
            )

            if (element.hasCaption) {
                TextRoomMessageTimelineElementView(holder, element, showActionMenu)
            }
        }
    }
}


@Composable
internal fun FileBasedView(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.FileBased<*>,
    onSave: () -> Unit,
    showActionMenu: () -> Unit,
    displayProgressOverElement: Boolean,
    isPreview: Boolean,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.(onShowActionMenu: () -> Unit, openElementDetails: () -> Unit) -> Unit
) {
    val hoverMessage = remember { mutableStateOf(false) }

    val elementDetailsFactory = DI.get<ElementDetailsViewSelector>().rememberFactory(element)
    var openElementDetails by remember { mutableStateOf(false) }
    Box {
        Column(
            Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            openElementDetails = true
                        },
                        onLongPress = { showActionMenu() },
                    )
                }
                .buttonPointerModifier()
                .then(
                    if (isPreview) Modifier
                    else Modifier.pointerMoveFilter(
                        onEnter = {
                            hoverMessage.value = true
                            true
                        }, onExit = {
                            hoverMessage.value = false
                            true
                        })
                )
        ) {
            Box(modifier = Modifier.width(IntrinsicSize.Min)) {
                Column {
                    // content based on the actual file
                    content(showActionMenu) {
                        openElementDetails = true
                    }
                }
                if (!isPreview) {
                    FileContentOverlay(
                        hoverMessage,
                        overlay,
                    )
                }
            }
            if (!displayProgressOverElement) {
                LoadingProgresses(holder, element, Modifier.align(Alignment.CenterHorizontally))
            }
        }
        if (displayProgressOverElement) {
            LoadingProgresses(holder, element, Modifier.align(Alignment.Center))
        }

    }
    if (openElementDetails && elementDetailsFactory != null) {
        elementDetailsFactory.create(element, onSave, onClose = { openElementDetails = false })
    }
}

@Composable
fun LoadingProgresses(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.FileBased<*>,
    modifier: Modifier
) {
    val downloadProgressElement = element.downloadMediaProgress.collectAsState().value
    val uploadProgress = holder.asOutboxElementHolder()?.uploadProgress?.collectAsState()?.value
    if (uploadProgress != null) {
        Box(modifier) {
            DownloadProgress(
                uploadProgress,
                cancel = {
                    if (holder is OutboxElementHolderViewModel) {
                        holder.abortSend()
                    }
                },
            )
        }
    }
    if (downloadProgressElement != null)
        Box(modifier) {
            DownloadProgress(
                downloadProgressElement,
                { element.cancelDownloadMedia() },
            )
        }
}
