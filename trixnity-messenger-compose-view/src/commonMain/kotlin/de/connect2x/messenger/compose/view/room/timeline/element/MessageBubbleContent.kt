package de.connect2x.messenger.compose.view.room.timeline.element

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.messenger.compose.view.pointerMoveFilter
import de.connect2x.messenger.compose.view.room.timeline.BaseTimelineElementHolderContextMenuAction
import de.connect2x.messenger.compose.view.room.timeline.OverflowingFileInfo
import de.connect2x.messenger.compose.view.room.timeline.OverflowingFileInfoDisplayMode.FILENAME_AND_INFO
import de.connect2x.messenger.compose.view.room.timeline.OverflowingFileInfoDisplayMode.FILENAME_ONLY
import de.connect2x.messenger.compose.view.room.timeline.formatFileMetadata
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.AudioMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.FileBasedMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ImageMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.VideoMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.formatDuration
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration.Companion.milliseconds

interface MessageBubbleContentView {
    @Composable
    fun create(
        roomMessageViewModel: RoomMessageViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        messageActions: List<BaseTimelineElementHolderContextMenuAction>,
        showBottomSheet: MutableState<Boolean>,
    )
}

@Composable
fun MessageBubbleContent(
    roomMessageViewModel: RoomMessageViewModel,
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    messageActions: List<BaseTimelineElementHolderContextMenuAction>,
    showBottomSheet: MutableState<Boolean>,
) {
    DI.current.get<MessageBubbleContentView>()
        .create(roomMessageViewModel, timelineElementHolderViewModel, messageActions, showBottomSheet)
}

class MessageBubbleContentViewImpl : MessageBubbleContentView {
    @Composable
    override fun create(roomMessageViewModel: RoomMessageViewModel, timelineElementHolderViewModel: BaseTimelineElementHolderViewModel, messageActions: List<BaseTimelineElementHolderContextMenuAction>, showBottomSheet: MutableState<Boolean>) {
        val hoverMessage = remember { mutableStateOf(false) }
        val sendError = remember {
            if (timelineElementHolderViewModel is OutboxElementHolderViewModel)
                timelineElementHolderViewModel.sendError
            else MutableStateFlow(null)
        }.collectAsState().value
        // small hack: as gesture detection (onTap) in children disable detection of gestures in parent components, we have
        // to propagate all gestures to one child where all detection takes place
        val onLongPress: (Offset) -> Unit = { _: Offset ->
            showBottomSheet.value = true
        }

        val highlight by remember {
            if (timelineElementHolderViewModel is TimelineElementHolderViewModel)
                timelineElementHolderViewModel.highlight
            else MutableStateFlow(false)
        }.collectAsState()

        val messageBackground =
            when {
                sendError != null -> MaterialTheme.colorScheme.errorContainer
                roomMessageViewModel.isByMe -> MaterialTheme.colorScheme.primary.copy(
                    alpha = if (timelineElementHolderViewModel is OutboxElementHolderViewModel) 0.8f else 1f
                )

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
                .pointerInput(timelineElementHolderViewModel) { // key is important to react to changes
                    detectTapGestures(onLongPress = onLongPress) // in case the child element has no tap / click detection, we can use this
                    size
                }
        ) {
            Row {
                if (roomMessageViewModel.isByMe.not()) {
                    if (roomMessageViewModel.showChatBubbleEdge) {
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
                            MessageHeader(roomMessageViewModel, timelineElementHolderViewModel)

                            when (roomMessageViewModel) {
                                is ImageMessageViewModel -> { //, is VideoElement -> {
                                    MessageContent(
                                        roomMessageViewModel,
                                        timelineElementHolderViewModel,
                                        onLongPress
                                    )
                                    MessageDate(roomMessageViewModel, timelineElementHolderViewModel)
                                }

                                else -> MessageAndDate(
                                    roomMessageViewModel,
                                    timelineElementHolderViewModel,
                                    onLongPress
                                )
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
                if (roomMessageViewModel.isByMe && roomMessageViewModel.showChatBubbleEdge) {
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
                        .padding(top = 4.dp, end = if (roomMessageViewModel.isByMe) 14.dp else 4.dp)
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
                        if (roomMessageViewModel is FileBasedMessageViewModel) {
                            Tooltip(
                                { TooltipText(formatFileMetadata(roomMessageViewModel)) }
                            ) {
                                OverflowingFileInfo(
                                    roomMessageViewModel,
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

                if (roomMessageViewModel is FileBasedMessageViewModel) {
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
                                when (roomMessageViewModel) {
                                    is ImageMessageViewModel -> {
                                        OverflowingFileInfo(
                                            roomMessageViewModel,
                                            FILENAME_ONLY,
                                            modifier = Modifier.weight(0.6f, false),
                                            color = metaDataPreviewColor,
                                        )
                                        Text(
                                            " (${roomMessageViewModel.fileSize?.let { formatSize(it.toLong()) }})",
                                            modifier = Modifier.weight(1.0f, false),
                                            color = metaDataPreviewColor,
                                            maxLines = 1,
                                        )
                                    }

                                    is VideoMessageViewModel -> {
                                        OverflowingFileInfo(
                                            roomMessageViewModel,
                                            FILENAME_ONLY,
                                            modifier = Modifier.weight(0.6f, false),
                                            color = metaDataPreviewColor,
                                        )
                                        Text(
                                            ": ${roomMessageViewModel.duration?.let { formatDuration(it.milliseconds) }} " +
                                                    "(${
                                                        roomMessageViewModel.fileSize?.let {
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
                                                roomMessageViewModel.duration?.let { formatDuration(it.milliseconds) }
                                            } ",
                                            Modifier.weight(0.6f, false),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = metaDataPreviewColor,
                                        )
                                        Text(
                                            "(${
                                                roomMessageViewModel.fileSize?.let {
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
