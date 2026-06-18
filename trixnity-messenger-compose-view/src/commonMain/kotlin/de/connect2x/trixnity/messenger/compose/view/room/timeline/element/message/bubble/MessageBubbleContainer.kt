package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.pointerMoveFilter
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.util.asOutboxElementHolder
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedActionMenuState
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel

@Composable
fun MessageBubbleContainer(
    holder: BaseTimelineElementHolderViewModel,
    needsMaxWidth: Boolean,
    reactionsOpen: MutableState<Boolean>,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
    isPreview: Boolean,
    isMentioned: Boolean,
    interactionSource: MutableInteractionSource,
    index: Int,
    onRedact: () -> Unit,
    content: @Composable (showActionMenu: () -> Unit) -> Unit,
) {
    val sendError = holder.asOutboxElementHolder()?.sendError?.collectAsState()?.value
    val isFirstInUserSequence = holder.isFirstInUserSequence.collectAsState().value == true
    val showActionMenu = remember { mutableStateOf<ThemedActionMenuState>(ThemedActionMenuState.Closed) }
    val hoverMessage = remember { mutableStateOf(false) }
    val i18n = DI.get<I18nView>()
    val element = holder.element.collectAsState().value
    val sender = holder.sender.collectAsState().value
    val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()

    val messagePadding =
        remember(holder.isByMe) { if (holder.isByMe) Modifier.padding(end = 8.dp) else Modifier.padding(start = 8.dp) }
    val messageBubbleStyle =
        when {
            sendError != null -> MaterialTheme.components.messageBubbleError
            holder.isByMe -> MaterialTheme.components.messageBubbleOwn
            else -> MaterialTheme.components.messageBubbleOther
        }

    Box(
        modifier =
            if (isPreview) Modifier
            else
                Modifier.pointerMoveFilter(
                        onEnter = {
                            hoverMessage.value = true
                            true
                        },
                        onExit = {
                            hoverMessage.value = false
                            true
                        },
                    )
                    .pointerInput(holder) { // key is important to react to changes
                        detectTapGestures(
                            onLongPress = { showActionMenu.value = ThemedActionMenuState.Anchored }
                        ) // in case the child element has no tap / click detection, we can use this
                        size
                    }
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val hoverInteractionSource = remember { MutableInteractionSource() }
        val actionMenuFocusState = remember { MutableInteractionSource() }
        val hasFocus = interactionSource.collectIsFocusedAsState().value
        val a11yLabel = buildAnnotatedString {
            if (!holder.isByMe && isMentioned) {
                append("${i18n.userWasMentioned()} ")
            }

            val senderName = sender?.name ?: i18n.commonUnknown()
            append(senderName)
            append(" (${holder.formattedTime}): ")

            element?.let { append(timelineElementViewSelector.a11yLabel(it, i18n)) }
        }
        ThemedSurface(
            style = messageBubbleStyle,
            focused = hasFocus,
            modifier =
                Modifier.then(messagePadding)
                    .drawWithCache {
                        onDrawBehind {
                            if (isFirstInUserSequence) {
                                drawChatEdge(holder.isByMe, messageBubbleStyle.color)
                            }
                        }
                    }
                    .focusable(true, interactionSource)
                    .hoverable(hoverInteractionSource)
                    .semantics {
                        collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                        this.text = a11yLabel
                    },
        ) {
            if (!isPreview) {
                MessageBubbleActionMenu(
                    holder = holder,
                    showActionMenu = showActionMenu,
                    onOpenMetadata = {
                        if (holder is TimelineElementHolderViewModel) holder.openTimelineElementMetadata()
                    },
                    onReactToMessage = { reactionsOpen.value = true },
                    hoverInteractionSource = hoverInteractionSource,
                    focusInteractionSource = actionMenuFocusState,
                    additionalContextActions = additionalContextActions,
                    onRedact = onRedact,
                )
            }

            MessageBubbleContent(
                holder,
                needsMaxWidth,
                isMentioned,
                { showActionMenu.value = ThemedActionMenuState.Anchored },
                content,
            )
        }
    }
}

private val ChatEdge =
    Path().apply {
        moveTo(x = -1f, y = 0f)
        lineTo(x = 0f, y = 1f)
        lineTo(x = 1f, y = 0f)
        close()
    }

fun DrawScope.drawChatEdge(isByMe: Boolean, color: Color) {
    if (isByMe) {
        translate(left = size.width) { scale(8.dp.toPx(), pivot = Offset.Zero) { drawPath(ChatEdge, color) } }
    } else {
        scale(8.dp.toPx(), pivot = Offset.Zero) { drawPath(ChatEdge, color) }
    }
}
