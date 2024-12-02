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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.messenger.compose.view.pointerMoveFilter
import de.connect2x.messenger.compose.view.room.timeline.element.MessageInfo
import de.connect2x.messenger.compose.view.room.timeline.element.MessageReactions
import de.connect2x.messenger.compose.view.room.timeline.element.ReadMarker
import de.connect2x.messenger.compose.view.room.timeline.element.util.asOutboxElementHolder
import de.connect2x.messenger.compose.view.room.timeline.element.util.asTimelineElementHolder
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import kotlinx.coroutines.launch

interface MessageBubbleView {
    @Composable
    fun create(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
        showDate: Boolean, // FIXME do we really need that? What is the use case?
        needsMaxWidth: Boolean,
        additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit = {},
        overlay: @Composable BoxScope.() -> Unit = {},
        content: @Composable () -> Unit,
    )
}

@Composable
fun MessageBubble(
    holder: BaseTimelineElementHolderViewModel,
    element: TimelineElementViewModel<*>,
    showDate: Boolean,
    needsMaxWidth: Boolean,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    DI.get<MessageBubbleView>()
        .create(holder, element, showDate, needsMaxWidth, additionalContextActions, overlay, content)
}

class MessageBubbleViewImpl : MessageBubbleView {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun create(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>,
        showDate: Boolean,
        needsMaxWidth: Boolean,
        additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
        overlay: @Composable BoxScope.() -> Unit,
        content: @Composable () -> Unit,
    ) {
        val redactionInProgress =
            holder.asTimelineElementHolder()?.redactionInProgress?.collectAsState()?.value == true
        val showBigGap = holder.showBigGapBefore.collectAsState().value == true
        val topPadding = if (showBigGap) 10.dp else 3.dp

        // FIXME downloads already in children?

        BoxWithConstraints(
            Modifier.fillMaxWidth()
        ) {
            val padding =
                (if (maxWidth < 400.dp) 20.dp else 80.dp) - (if (redactionInProgress) 16.dp else 0.dp)
            Column(
                modifier = Modifier.run {
                    if (holder.isByMe) padding(start = padding, top = topPadding)
                        .align(Alignment.CenterEnd)
                    else padding(end = padding, top = topPadding)
                },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = if (holder.isByMe) Alignment.End else Alignment.Start,
            ) {
                Row {
                    if (redactionInProgress) {
                        val i18n = DI.get<I18nView>()
                        Box(Modifier.size(16.dp).padding(2.dp)) {
                            Icon(Icons.Default.AutoDelete, i18n.messageBubbleBeingDeleted())
                        }
                    }
                    MessageBubbleContainer(
                        holder,
                        showDate,
                        needsMaxWidth,
                        additionalContextActions,
                        overlay,
                        content,
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubbleContainer(
    holder: BaseTimelineElementHolderViewModel,
    showDate: Boolean,
    needsMaxWidth: Boolean,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
    overlay: @Composable BoxScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val sendError = holder.asOutboxElementHolder()?.sendError?.collectAsState()?.value
    val hoverMessage = remember { mutableStateOf(false) }
    val infoOpen = remember { mutableStateOf(false) }
    val reactionsOpen = remember { mutableStateOf(false) }

    val messageBackground =
        when {
            sendError != null -> MaterialTheme.colorScheme.errorContainer
            holder.isByMe -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondary
        }

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
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = messageBackground
            ) {
                Box(Modifier.fillMaxSize()) {
                    MessageBubbleContent(holder, showDate, needsMaxWidth, content)
                    MessageOverlay(
                        hoverMessage,
                        overlay,
                    )
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

        ActionMenu(
            holder,
            hoverMessage,
            { infoOpen.value = true },
            { reactionsOpen.value = true },
            additionalContextActions,
        )

        MessageInfo(
            holder,
            infoOpen,
            modifier = Modifier.padding(start = 8.dp),
        )

        MessageReactions(
            holder,
            reactionsOpen,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
fun MessageBubbleContent(
    holder: BaseTimelineElementHolderViewModel,
    showDate: Boolean,
    needsMaxWidth: Boolean,
    content: @Composable () -> Unit,
) {
    val i18n = DI.get<I18nView>()

    val highlight = holder.asTimelineElementHolder()?.highlight?.collectAsState()?.value == true
    val sendError = holder.asOutboxElementHolder()?.sendError?.collectAsState()?.value

    val highlighted = if (highlight) Modifier.border(
        width = 3.dp,
        color = MaterialTheme.colorScheme.outline,
        shape = RoundedCornerShape(8.dp),
    ) else Modifier
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
                // FIXME insert Reply here?
                content()
                if (showDate) {
                    Row(
                        Modifier.align(Alignment.End).padding(5.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        holder.formattedTime.let {
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
                        ReadMarker(holder)
                    }
//                                                if (holder.formattedTime == null) {  FIXME
//                                                    Spacer(Modifier.defaultMinSize(minWidth = 45.dp))
//                                                }
                }
            } else {
                Layout(content = {
                    content()
                    if (showDate) {
                        holder.formattedTime.let {
                            Row(
                                modifier = Modifier.padding(
                                    start = 5.dp,
                                    end = 5.dp,
                                    bottom = 5.dp
                                ),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                if (holder is TimelineElementHolderViewModel) {
                                    val isReplaced = holder.isReplaced.collectAsState()
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
                                ReadMarker(holder)
                            }
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
                                date.place(
                                    message.width + spacing,
                                    height - date.height
                                )
                            }
                        } else {
                            layout(
                                width = constraints.maxWidth,
                                height = message.height + date.height
                            ) {
                                message.place(0, 0)
                                date.place(
                                    constraints.maxWidth - date.width,
                                    message.height
                                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.ActionMenu(
    holder: BaseTimelineElementHolderViewModel,
    hoverMessage: State<Boolean>,
    onInfo: () -> Unit,
    onReact: () -> Unit,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
) {
    val i18n = DI.current.get<I18nView>()
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(false)
    var showBottomSheet by remember { mutableStateOf(false) }
    var isContextMenuOpen by remember { mutableStateOf(false) }

    if (Platform.current.isMobile) {
        val onClose = {
            coroutineScope.launch {
                bottomSheetState.hide()
            }.invokeOnCompletion {
                if (!bottomSheetState.isVisible)
                    showBottomSheet = false
            }
            Unit
        }
        if (showBottomSheet) {
            ModalBottomSheet(
                sheetState = bottomSheetState,
                onDismissRequest = { showBottomSheet = false },
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 100.dp)
                        .padding(bottom = 40.dp)
                ) {
                    additionalContextActions(onClose)
                    holder.baseMenuActions(i18n, onInfo, onReact).forEach { action ->
                        action.render {
                            onClose()
                        }
                    }
                }
            }
        }
    } else { // not mobile
        val onClose = {
            isContextMenuOpen = false
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = 4.dp,
                    end = if (holder.isByMe) 14.dp else 4.dp
                )
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
                        .clickable { isContextMenuOpen = isContextMenuOpen.not() }
                        .pointerHoverIcon(PointerIcon.Hand)
                        .indication(
                            indication = null,
                            interactionSource = MutableInteractionSource()
                        )
                ) {
                    Icon(Icons.Default.ExpandMore, "Kontextmenü", tint = Color.White)     // FIXME i18n
                }
            }
            DropdownMenu(
                expanded = isContextMenuOpen,
                onDismissRequest = { isContextMenuOpen = false },
                offset = DpOffset(0.dp, 0.dp),
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
                    .sizeIn(maxWidth = 300.dp),
            ) {
                additionalContextActions(onClose)
                holder.baseMenuActions(i18n, onInfo, onReact).forEach { action ->
                    action.render { onClose() }
                }
            }
        }
    }
}

@Composable
fun BoxScope.MessageOverlay(hoverMessage: State<Boolean>, overlay: @Composable BoxScope.() -> Unit) {
    AnimatedVisibility(
        hoverMessage.value,
        Modifier
            .align(Alignment.BottomStart)
            .padding(5.dp),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.messengerColors.metaDataPreviewBackground)
                .padding(6.dp)
        ) {
            overlay()
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
