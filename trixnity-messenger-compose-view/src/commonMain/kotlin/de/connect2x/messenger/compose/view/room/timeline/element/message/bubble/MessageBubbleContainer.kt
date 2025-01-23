package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.gesturesDisabled
import de.connect2x.messenger.compose.view.pointerMoveFilter
import de.connect2x.messenger.compose.view.room.timeline.element.util.asOutboxElementHolder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel


@Composable
fun MessageBubbleContainer(
    holder: BaseTimelineElementHolderViewModel,
    config: MessageBubbleDisplayConfig,
    reactionsOpen: MutableState<Boolean>,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable (showActionMenu: () -> Unit) -> Unit,
) {
    val isFirstInUserSequence = holder.isFirstInUserSequence.collectAsState().value == true
    val sendError = holder.asOutboxElementHolder()?.sendError?.collectAsState()?.value
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
                    size
                }
        ) {
            Row {
                if (holder.isByMe.not()) {
                    if (showChatBubbleTail) {
                        Box(
                            Modifier
                                .background(
                                    messageBackground,
                                    shape = ChatBubbleTailLeft(with(LocalDensity.current) { 8.dp.roundToPx() }),
                                )
                                .requiredWidth(8.dp)
                                .fillMaxHeight()
                        )
                    } else {
                        Spacer(Modifier.requiredWidth(8.dp))
                    }
                }
                var contentSize: IntSize by remember { mutableStateOf(IntSize.Zero) }
                Surface(
                    modifier = Modifier
                        .onSizeChanged { contentSize = it },
                    shape = RoundedCornerShape(8.dp),
                    color = messageBackground,
                ) {
                    Box(modifier = Modifier.width(IntrinsicSize.Max)) {
                        MessageBubbleContent(
                            holder, config,
                            { showActionMenu.value = true },
                            content,
                        )
                        MessageBubbleContentOverlay(
                            hoverMessage,
                            overlay,
                        )
                    }
                    if (config.preventUserInput) Box(
                        Modifier
                            .gesturesDisabled()
                            .buttonPointerModifier(enabled = false)
                            .size(
                                contentSize.width.dp / density.density,
                                contentSize.height.dp / density.density,
                            )
                    )
                }
                if (holder.isByMe && showChatBubbleTail) Box(
                    Modifier
                        .background(
                            messageBackground,
                            shape = ChatBubbleTailRight(with(LocalDensity.current) { 8.dp.roundToPx() }),
                        )
                        .zIndex(-1f)
                        .fillMaxHeight()
                    // No width and no padding, as really wide messages will push this
                    // to the max amount (we only use padding in the Timeline).
                )
            }
            if (config.showContextActionMenu) MessageBubbleActionMenu(
                holder,
                hoverMessage,
                showActionMenu,
                onMessageInfo = {
                    // TODO maybe add showMessageMetadata to BaseTimelineHolderElementVM?
                    if (holder is TimelineElementHolderViewModel) holder.openMessageMetadata()
                },
                onReactToMessage = { reactionsOpen.value = true },
                additionalContextActions,
            )
        }
    }
}


class ChatBubbleTailRight(private val offset: Int) : Shape {

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

class ChatBubbleTailLeft(private val offset: Int) : Shape {

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
