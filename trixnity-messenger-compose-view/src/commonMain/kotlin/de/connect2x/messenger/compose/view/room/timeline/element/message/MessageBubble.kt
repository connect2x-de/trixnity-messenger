package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.files.SaveFileDialog
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.messenger.compose.view.pointerMoveFilter
import de.connect2x.messenger.compose.view.room.timeline.OverflowingFileInfo
import de.connect2x.messenger.compose.view.room.timeline.OverflowingFileInfoDisplayMode.FILENAME_AND_INFO
import de.connect2x.messenger.compose.view.room.timeline.OverflowingFileInfoDisplayMode.FILENAME_ONLY
import de.connect2x.messenger.compose.view.room.timeline.element.BaseTimelineElementHolderContextMenuAction
import de.connect2x.messenger.compose.view.room.timeline.element.MessageContent
import de.connect2x.messenger.compose.view.room.timeline.element.ReadMarker
import de.connect2x.messenger.compose.view.room.timeline.formatFileMetadata
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.MessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.FileBasedMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.ImageRoomMessageTimelineElementViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.util.formatDuration
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

interface MessageBubbleView {
    @Composable
    fun create(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
        showDate: Boolean,
        needsMaxWidth: Boolean,
    )
}

@Composable
fun MessageBubble(
    holder: BaseTimelineElementHolderViewModel,
    element: TimelineElementViewModel<*>,
    showDate: Boolean,
    needsMaxWidth: Boolean,
    content: @Composable () -> Unit,
) {
    DI.get<MessageBubbleView>().create(holder, element, showDate, needsMaxWidth)
}

class MessageBubbleViewImpl : MessageBubbleView {
    @Composable
    override fun create(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
        showDate: Boolean,
        needsMaxWidth: Boolean,
    ) {
        val hoverMessage = remember { mutableStateOf(false) }
        val showMenu = remember { mutableStateOf(false) }
        val sendError = remember {
            if (holder is OutboxElementHolderViewModel) holder.sendError
            else MutableStateFlow(null)
        }.collectAsState().value
        // small hack: as gesture detection (onTap) in children disable detection of gestures in parent components, we have
        // to propagate all gestures to one child where all detection takes place
        val onLongPress: (Offset) -> Unit = remember {
            { _: Offset ->
                showMenu.value = true
            }
        }

        val highlight by remember {
            if (holder is TimelineElementHolderViewModel) holder.highlight
            else MutableStateFlow(false)
        }.collectAsState()

        val messageBackground =
            when {
                sendError != null -> MaterialTheme.colorScheme.errorContainer
                holder.isByMe -> MaterialTheme.colorScheme.primary

                else -> MaterialTheme.colorScheme.secondary
            }
        val metaDataPreviewColor = MaterialTheme.messengerColors.metaDataPreview
        Box(
            Modifier
                .pointerMoveFilter(
                    onEnter = {
                        hoverMessage.value = true
                        true
                    }, onExit = {
                        hoverMessage.value = false
                        true
                    })
                .pointerInput(holder) { // key is important to react to changes
                    detectTapGestures(onLongPress = onLongPress) // in case the child element has no tap / click detection, we can use this
                    size
                }
        ) {
            Row {
                val isFirstInUserSequence = holder.isFirstInUserSequence.collectAsState().value == true
                if (holder.isByMe.not()) {
                    if (isFirstInUserSequence) {
                        Box(
                            Modifier
                                .background(
                                    messageBackground,
                                    shape = ChatEdgeLeft(with(LocalDensity.current) { 8.dp.roundToPx() })
                                )
                                .requiredWidth(8.dp)
                                .fillMaxHeight()
                        )
                    } else {
                        Spacer(Modifier.requiredWidth(8.dp))
                    }
                }
                val highlighted = if (highlight) Modifier.border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp),
                ) else Modifier
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = messageBackground
                ) {
                    Row {
                        if (sendError != null) {
                            Icon(
                                Icons.Default.Warning, "send error",
                                Modifier.padding(5.dp).align(Alignment.CenterVertically)
                            )
                        }
                        Column(
                            Modifier
                                .padding(0.dp)
                                .weight(1.0f, fill = false)
                                .then(highlighted)
                        ) {
                            val showSender = holder.showSender.collectAsState().value == true
                            val sender = holder.sender.collectAsState().value

                            if (showSender && sender != null) { // FIXME placeholder?
                                Box(
                                    Modifier
                                        .padding(start = 10.dp, end = 10.dp, top = 10.dp)
                                ) {
                                    Text(
                                        text = sender.name,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = MaterialTheme.messengerColors.getUserColor(
                                                sender.userId
                                            )
                                        ),
                                    )
                                }
                            }

                            if (needsMaxWidth) {
                                MessageContent(
                                    messageTimelineElementViewModel,
                                    holder,
                                    onLongPress
                                )
                                if (showDate) {
                                    Row(
                                        Modifier.align(Alignment.End).padding(5.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        messageTimelineElementViewModel.formattedTime?.let {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight(),
                                                contentAlignment = Alignment.BottomEnd
                                            ) {
                                                Text(
                                                    it,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.paddingFromBaseline(0.dp),
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                        ReadMarker(messageTimelineElementViewModel, timelineElementHolderViewModel)
                                    }
                                    if (messageTimelineElementViewModel.formattedTime == null) {
                                        Spacer(Modifier.defaultMinSize(minWidth = 45.dp))
                                    }
                                }
                            } else {
                                Layout(content = {
                                    MessageContent(
                                        messageTimelineElementViewModel,
                                        timelineElementHolderViewModel,
                                        onLongPress
                                    )
                                    if (showDate) {
                                        messageTimelineElementViewModel.formattedTime?.let {
                                            Row(
                                                modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 5.dp),
                                                verticalAlignment = Alignment.Bottom,
                                            ) {
                                                if (timelineElementHolderViewModel is TimelineElementHolderViewModel) {
                                                    val isReplaced =
                                                        timelineElementHolderViewModel.isReplaced.collectAsState()
                                                    if (isReplaced.value)
                                                        Text(
                                                            i18n.messageBubbleEdited(),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.paddingFromBaseline(0.dp)
                                                                .padding(end = 2.dp),
                                                            maxLines = 1,
                                                        )
                                                }
                                                Text(
                                                    it,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.paddingFromBaseline(0.dp),
                                                    maxLines = 1,
                                                )
                                                ReadMarker(
                                                    messageTimelineElementViewModel,
                                                    timelineElementHolderViewModel
                                                )
                                            }
                                        }
                                        if (messageTimelineElementViewModel.formattedTime == null) {
                                            Spacer(Modifier.defaultMinSize(minWidth = 45.dp))
                                        }
                                    }
                                }) { elements, constraints ->
                                    val spacing = 10.dp.roundToPx()
                                    val message = elements[0].measure(constraints)
                                    val date = elements.getOrNull(1)?.measure(constraints)

                                    date?.let {
                                        if (message.width + spacing + date.width < constraints.maxWidth) {
                                            // add extra padding to bottom that is missing otherwise
                                            val height = message.height + 10.dp.roundToPx()
                                            layout(
                                                width = message.width + spacing + date.width,
                                                height = height,
                                            ) {
                                                message.place(0, 0)
                                                date.place(message.width + spacing, height - date.height)
                                            }
                                        } else {
                                            layout(
                                                width = constraints.maxWidth,
                                                height = message.height + date.height
                                            ) {
                                                message.place(0, 0)
                                                date.place(constraints.maxWidth - date.width, message.height)
                                            }
                                        }
                                    } ?: layout(
                                        message.width,
                                        message.height
                                    ) {
                                        message.place(0, 0)
                                    }
                                }
                            }

                            if (sendError != null) {
                                Box(Modifier.padding(start = 10.dp, end = 10.dp)) {
                                    Text(
                                        text = sendError,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
                if (holder.isByMe && isFirstInUserSequence) {
                    Box(
                        Modifier
                            .background(
                                messageBackground,
                                shape = ChatEdgeRight(with(LocalDensity.current) { 8.dp.roundToPx() })
                            )
                            .zIndex(-1f)
                            .fillMaxHeight()
                        // no width and no padding, as really wide messages will push this to the max amount (we only use padding in the Timeline)
                    )
                }
            }
            if (Platform.current.isMobile.not() && messageActions.isNotEmpty()) {
                val isContextMenuOpen = remember { mutableStateOf(false) }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = if (messageTimelineElementViewModel.isByMe) 14.dp else 4.dp)
                        .defaultMinSize(minHeight = 24.dp, minWidth = 24.dp) // 24dp Material Icon
                ) {
                    AnimatedVisibility(
                        hoverMessage.value,
                        Modifier
                            .clip(CircleShape)
                    ) {
                        Box(
                            Modifier
                                .background(Color.Black.copy(alpha = 0.1f))
                                .clickable { isContextMenuOpen.value = isContextMenuOpen.value.not() }
                                .pointerHoverIcon(PointerIcon.Hand)
                                .indication(
                                    indication = null,
                                    interactionSource = MutableInteractionSource()
                                )
                        ) {
                            Icon(Icons.Default.ExpandMore, "Kontextmenü", tint = Color.White)
                        }
                    }
                    DropdownMenu(
                        expanded = isContextMenuOpen.value,
                        onDismissRequest = { isContextMenuOpen.value = false },
                        offset = DpOffset(0.dp, 0.dp),
                        modifier = Modifier.background(MaterialTheme.colorScheme.background)
                            .sizeIn(maxWidth = 300.dp),
                    ) {
                        if (messageTimelineElementViewModel is FileBasedMessageViewModel) {
                            Tooltip(
                                { TooltipText(formatFileMetadata(messageTimelineElementViewModel)) }
                            ) {
                                OverflowingFileInfo(
                                    messageTimelineElementViewModel,
                                    FILENAME_AND_INFO,
                                    modifier = Modifier.padding(5.dp),
                                )
                            }
                            if (messageActions.isNotEmpty()) {
                                HorizontalDivider()
                            }
                        }
                        messageActions.forEach { action ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        action.label,
                                        Modifier.buttonPointerModifier(),
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                },
                                onClick = {
                                    isContextMenuOpen.value = false
                                    action()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp),
                            )
                        }
                    }
                }

                if (messageTimelineElementViewModel is FileBasedMessageViewModel) {
                    AnimatedVisibility(
                        hoverMessage.value,
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(5.dp),
                        enter = fadeIn(),
                        exit = fadeOut(),
                    )
                    {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.messengerColors.metaDataPreviewBackground)
                                .padding(6.dp)
                        ) {
                            Row {
                                when (messageTimelineElementViewModel) {
                                    is ImageRoomMessageTimelineElementViewModelImpl -> {
                                        OverflowingFileInfo(
                                            messageTimelineElementViewModel,
                                            FILENAME_ONLY,
                                            modifier = Modifier.weight(0.6f, false),
                                            color = metaDataPreviewColor,
                                        )
                                        Text(
                                            " (${messageTimelineElementViewModel.fileSize?.let { formatSize(it.toLong()) }})",
                                            modifier = Modifier.weight(1.0f, false),
                                            color = metaDataPreviewColor,
                                            maxLines = 1,
                                        )
                                    }

                                    is VideoMessageViewModel -> {
                                        OverflowingFileInfo(
                                            messageTimelineElementViewModel,
                                            FILENAME_ONLY,
                                            modifier = Modifier.weight(0.6f, false),
                                            color = metaDataPreviewColor,
                                        )
                                        Text(
                                            ": ${messageTimelineElementViewModel.duration?.let { formatDuration(it.milliseconds) }} " +
                                                    "(${
                                                        messageTimelineElementViewModel.fileSize?.let {
                                                            formatSize(
                                                                it.toLong()
                                                            )
                                                        }
                                                    })",
                                            Modifier.weight(0.4f, false),
                                            color = metaDataPreviewColor,
                                        )
                                    }

                                    is AudioMessageViewModel -> {
                                        Text(
                                            "${
                                                messageTimelineElementViewModel.duration?.let { formatDuration(it.milliseconds) }
                                            } ",
                                            Modifier.weight(0.6f, false),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = metaDataPreviewColor,
                                        )
                                        Text(
                                            "(${
                                                messageTimelineElementViewModel.fileSize?.let {
                                                    formatSize(
                                                        it.toLong()
                                                    )
                                                }
                                            })",
                                            Modifier.weight(1.0f, false),
                                            color = metaDataPreviewColor,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaveDialogForFileDownload( // FIXME in factories
    roomMessageViewModel: FileBasedMessageViewModel,
    downloadAction: MutableState<Boolean>
) {
    val error = roomMessageViewModel.downloadError.collectAsState().value
    if (downloadAction.value)
        SaveFileDialog(
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
    messageTimelineElementViewModel: MessageTimelineElementViewModel,
) {
    val bottomSheetState = rememberModalBottomSheetState(false)
    val coroutineScope = rememberCoroutineScope()
    val isFileBased = messageTimelineElementViewModel is FileBasedMessageViewModel
    val hasMessageActions = messageActions.isNotEmpty()
    if (Platform.current.isMobile && (hasMessageActions || isFileBased)) {
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
                        Tooltip({ TooltipText(formatFileMetadata(messageTimelineElementViewModel)) }) {
                            OverflowingFileInfo(
                                messageTimelineElementViewModel as FileBasedMessageViewModel,
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
