package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.connect2x.messenger.compose.view.pointerMoveFilter
import de.connect2x.messenger.compose.view.room.timeline.element.util.asOutboxElementHolder
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel

@Composable
fun MessageBubbleContainer(
    holder: BaseTimelineElementHolderViewModel,
    needsMaxWidth: Boolean,
    reactionsOpen: MutableState<Boolean>,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
    isPreview: Boolean,
    content: @Composable (showActionMenu: () -> Unit) -> Unit,
) {
    val sendError = holder.asOutboxElementHolder()?.sendError?.collectAsState()?.value
    val isFirstInUserSequence = holder.isFirstInUserSequence.collectAsState().value == true
    val showActionMenu = remember { mutableStateOf(false) }
    val hoverMessage = remember { mutableStateOf(false) }

    val messageBubbleStyle = when {
        sendError != null -> MaterialTheme.components.messageBubbleError
        holder.isByMe -> MaterialTheme.components.messageBubbleOwn
        else -> MaterialTheme.components.messageBubbleOther
    }

    Column {
        Box(
            modifier = if (isPreview) Modifier else Modifier
                .pointerMoveFilter(
                    onEnter = {
                        hoverMessage.value = true
                        true
                    }, onExit = {
                        hoverMessage.value = false
                        true
                    })
                .pointerInput(holder) { // key is important to react to changes
                    detectTapGestures(onLongPress = {
                        showActionMenu.value = true
                    }) // in case the child element has no tap / click detection, we can use this
                    size
                }
        ) {
            Row {
                if (holder.isByMe.not()) {
                    if (isFirstInUserSequence) {
                        Box(
                            Modifier
                                .background(
                                    messageBubbleStyle.color,
                                    shape = ChatEdgeLeft(with(LocalDensity.current) { 8.dp.roundToPx() })
                                )
                                .requiredWidth(8.dp)
                        )
                    } else {
                        Spacer(Modifier.requiredWidth(8.dp))
                    }
                }
                ThemedSurface(
                    style = messageBubbleStyle,
                ) {
                    Box(modifier = Modifier.width(IntrinsicSize.Max)) {
                        MessageBubbleContent(holder, needsMaxWidth, { showActionMenu.value = true }, content)
                    }
                }
                if (holder.isByMe && isFirstInUserSequence) {
                    Box(
                        Modifier
                            .background(
                                messageBubbleStyle.color,
                                shape = ChatEdgeRight(with(LocalDensity.current) { 8.dp.roundToPx() })
                            )
                            .zIndex(-1f)
                        // no width and no padding, as really wide messages will push this to the max amount (we only use padding in the Timeline)
                    )
                }
            }

            if (!isPreview) {
                MessageBubbleActionMenu(
                    holder,
                    hoverMessage,
                    showActionMenu,
                    onOpenMetadata = {
                        if (holder is TimelineElementHolderViewModel) holder.openTimelineElementMetadata()
                    },
                    onReactToMessage = { reactionsOpen.value = true },
                    additionalContextActions,
                )
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
