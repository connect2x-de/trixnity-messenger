package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.files.SaveDialog
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.messenger.compose.view.room.timeline.BaseTimelineElementHolderContextMenuAction
import de.connect2x.messenger.compose.view.room.timeline.OverflowingFileInfo
import de.connect2x.messenger.compose.view.room.timeline.OverflowingFileInfoDisplayMode.FILENAME_AND_INFO
import de.connect2x.messenger.compose.view.room.timeline.formatFileMetadata
import de.connect2x.messenger.compose.view.room.timeline.getContextMenuActions
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.FileBasedMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomMessageViewModel
import kotlinx.coroutines.launch

// TODO TIM
interface MessageBubbleView {
    @Composable
    fun create(
        roomMessageViewModel: RoomMessageViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    )
}

@Composable
fun MessageBubble(
    roomMessageViewModel: RoomMessageViewModel,
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
) {
    DI.get<MessageBubbleView>().create(roomMessageViewModel, timelineElementHolderViewModel)
}

class MessageBubbleViewImpl : MessageBubbleView {
    @Composable
    override fun create(
        roomMessageViewModel: RoomMessageViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel
    ) {
        val i18n = DI.get<I18nView>()
        val downloadAction = remember { mutableStateOf(false) }
        val debugAction = remember { mutableStateOf(false) }

        val messageActions by timelineElementHolderViewModel.getContextMenuActions(
            i18n = i18n,
            downloadAction = { downloadAction.value = true },
            debugAction = { debugAction.value = true }
        )

        val showBottomSheet = remember { mutableStateOf(false) }
        ShowBottomSheet(messageActions, showBottomSheet, roomMessageViewModel)

        MessageBubbleContent(
            roomMessageViewModel,
            timelineElementHolderViewModel,
            messageActions,
            showBottomSheet,
        )

        if (roomMessageViewModel is FileBasedMessageViewModel) {
            SaveDialogForFileDownload(roomMessageViewModel, downloadAction)
        }

        if (debugAction.value) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                Row(Modifier.fillMaxSize()) {
                    SelectionContainer {
                        Text(timelineElementHolderViewModel.key)
                    }
                    Spacer(Modifier.size(20.dp))
                    Button({ debugAction.value = false }) {
                        Text(i18n.commonClose().capitalize(Locale.current))
                    }
                }
            }
        }
    }
}

@Composable
fun SaveDialogForFileDownload(
    roomMessageViewModel: FileBasedMessageViewModel,
    downloadAction: MutableState<Boolean>
) {
    val error = roomMessageViewModel.downloadError.collectAsState().value
    if (downloadAction.value)
        SaveDialog(
            roomMessageViewModel.fileName,
            roomMessageViewModel.fileMimeType,
            error,
            roomMessageViewModel::downloadFile,
            onCloseSaveFileDialog = {
                roomMessageViewModel.closeSaveFileDialog()
                downloadAction.value = false // only reset here as scope for SaveDialog has to be kept
            },
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowBottomSheet(
    messageActions: List<BaseTimelineElementHolderContextMenuAction>,
    showBottomSheet: MutableState<Boolean>,
    roomMessageViewModel: RoomMessageViewModel,
) {
    val coroutineScope = rememberCoroutineScope()
    val isFileBased = roomMessageViewModel is FileBasedMessageViewModel
    val hasMessageActions = messageActions.isNotEmpty()
    if (Platform.current.isMobile && (hasMessageActions || isFileBased)) {
        val bottomSheetState = rememberModalBottomSheetState(false)
        if (showBottomSheet.value)
            ModalBottomSheet(
                sheetState = bottomSheetState,
                onDismissRequest = { showBottomSheet.value = false },
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 100.dp)
                        .padding(bottom = 40.dp)
                ) {
                    if (isFileBased) {
                        Tooltip({ TooltipText(formatFileMetadata(roomMessageViewModel)) }) {
                            OverflowingFileInfo(
                                roomMessageViewModel as FileBasedMessageViewModel,
                                FILENAME_AND_INFO,
                                modifier = Modifier.padding(5.dp),
                            )
                        }
                        if (hasMessageActions) {
                            HorizontalDivider()
                        }
                    }

                    messageActions.forEach { action ->
                        Text(
                            action.label,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth()
                                .clickable {
                                    action()
                                    coroutineScope.launch {
                                        bottomSheetState.hide()
                                    }.invokeOnCompletion {
                                        if (!bottomSheetState.isVisible)
                                            showBottomSheet.value = false
                                    }
                                },
                        )
                    }
                }
            }
    }
}

class ChatEdgeRight(private val offset: Int) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val trianglePath = Path().apply {
            moveTo(x = 0f - offset, y = 0f)
            lineTo(x = 0f, y = 0f + offset)
            lineTo(x = 0f + offset, y = 0f)
        }
        return Outline.Generic(path = trianglePath)
    }
}

class ChatEdgeLeft(private val offset: Int) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val trianglePath = Path().apply {
            moveTo(x = 0f + offset * 2, y = 0f)
            lineTo(x = 0f + offset, y = 0f + offset)
            lineTo(x = 0f, y = 0f)
        }
        return Outline.Generic(path = trianglePath)
    }
}
