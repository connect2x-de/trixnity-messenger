package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.blockPointerInput
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.pointerMoveFilter
import de.connect2x.messenger.compose.view.room.timeline.element.util.asOutboxElementHolder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel


private val minifiedContentHeight = 355.dp
private val minifiedFadeoutHeight = 96.dp // Must be smaller than minifiedContentHeight!

@Composable
fun MessageBubbleContainer(
    holder: BaseTimelineElementHolderViewModel,
    config: MessageBubbleDisplayConfig,
    infoOpen: MutableState<Boolean>,
    reactionsOpen: MutableState<Boolean>,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable (onOpenActionMenu: () -> Unit) -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val isFirstInUserSequence = holder.isFirstInUserSequence.collectAsState().value == true
    val sendError = holder.asOutboxElementHolder()?.sendError?.collectAsState()?.value
    var contentExpanded by remember { mutableStateOf(false) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    val showActionMenu = remember { mutableStateOf(false) }
    val hoverMessage = remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val messageBackground = when {
        sendError != null -> MaterialTheme.colorScheme.errorContainer
        holder.isByMe -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
    val forceChatBubbleTail = config.alwaysShowChatBubbleTail
    val showChatBubbleTail = forceChatBubbleTail || isFirstInUserSequence
    val showMinifyControls = config.minifyBubble &&
            contentSize.height > with(density) { minifiedContentHeight.roundToPx() }

    Column {
        Box(
            if (config.preventUserInput) Modifier
            else Modifier
                .pointerMoveFilter(
                    onEnter = {
                        hoverMessage.value = true
                        true
                    }, onExit = {
                        hoverMessage.value = false
                        true
                    })
                .pointerInput(holder) { // Key is important to react to changes!
                    detectTapGestures(onLongPress = {
                        showActionMenu.value = true
                    }) // Catch input here in case the child element has no tap / click detection.
                }
        ) {
            Row {
                if (holder.isByMe.not()) {
                    if (showChatBubbleTail) {
                        Box(
                            Modifier
                                .background(
                                    messageBackground,
                                    shape = ChatBubbleTailLeft(with(density) { 8.dp.roundToPx() }),
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
                    color = messageBackground,
                ) {
                    Box(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .onSizeChanged { contentSize = it }
                    ) {
                        MessageBubbleContent(
                            holder, config,
                            onOpenActionMenu = { showActionMenu.value = true },
                            bottomSpacing = if (config.minifyBubble && contentExpanded) 32.dp else 0.dp,
                            content = {
                                Box(
                                    modifier = Modifier.heightIn(
                                        min = Dp.Unspecified,
                                        max = if (contentExpanded) Dp.Unspecified else minifiedContentHeight,
                                    )
                                ) {
                                    content {}
                                    if (config.minifyBubble && !contentExpanded) MessageFadeout(messageBackground)
                                }
                            })
                        MessageBubbleContentOverlay(
                            hoverMessage,
                            overlay,
                        )
                    }
                    if (config.preventUserInput) Box(
                        modifier = with(density) {
                            Modifier
                                .blockPointerInput()
                                .size(
                                    contentSize.width.toDp(),
                                    contentSize.height.toDp(),
                                )
                        }
                    )
                }
                if (holder.isByMe && showChatBubbleTail) Box(
                    Modifier
                        .background(
                            messageBackground,
                            shape = ChatBubbleTailRight(with(density) { 8.dp.roundToPx() }),
                        )
                        .zIndex(-1f)
                        .fillMaxHeight()
                    // No width and no padding, as really wide messages will push this
                    // to the max amount (we only use padding in the Timeline).
                )
            }
            if (config.enableContextActionMenu) MessageBubbleActionMenu(
                holder,
                hoverMessage,
                showActionMenu,
                onOpenInfo = { infoOpen.value = true },
                onReactToMessage = { reactionsOpen.value = true },
                additionalContextActions,
            )
            if (showMinifyControls) FloatingActionButton(
                onClick = { contentExpanded = !contentExpanded },
                modifier = Modifier
                    .size(40.dp)
                    .offset(y = (-8).dp)
                    .buttonPointerModifier()
                    .align(Alignment.BottomCenter)
                    .indication(
                        indication = null,
                        interactionSource = MutableInteractionSource()
                    ),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                contentColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                if (contentExpanded) Icon(Icons.Default.KeyboardArrowUp, i18n.commonCollapse())
                else Icon(Icons.Default.KeyboardArrowDown, i18n.commonExpand())
            }
        }
    }
}

@Composable
private fun BoxScope.MessageFadeout(messageBackground: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(minifiedFadeoutHeight)
            .align(Alignment.BottomCenter)
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        messageBackground.copy(alpha = 0f),
                        messageBackground,
                    )
                )
            )
    )
}

class ChatBubbleTailRight(private val offset: Int) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val trianglePath = Path().apply {
            moveTo(x = 0f - offset, y = 0f)
            lineTo(x = 0f, y = 0f + offset)
            lineTo(x = 0f + offset, y = 0f)
        }
        return Outline.Generic(path = trianglePath)
    }
}

class ChatBubbleTailLeft(private val offset: Int) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val trianglePath = Path().apply {
            moveTo(x = 0f + offset * 2, y = 0f)
            lineTo(x = 0f + offset, y = 0f + offset)
            lineTo(x = 0f, y = 0f)
        }
        return Outline.Generic(path = trianglePath)
    }
}
