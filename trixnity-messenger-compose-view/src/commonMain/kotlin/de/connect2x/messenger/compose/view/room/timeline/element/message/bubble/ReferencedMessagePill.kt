package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel

@Composable
fun ReferencedMessagePill(
    holder: TimelineElementHolderViewModel,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
    content: @Composable () -> Unit,
) {
    val sender = holder.sender.collectAsState().value
    val sendError = holder.sendError.collectAsState().value
    val messageBubbleStyle = when {
        sendError != null -> MaterialTheme.components.messageBubbleError
        holder.isByMe -> MaterialTheme.components.messageBubbleOwn
        else -> MaterialTheme.components.messageBubbleOther
    }
    val senderNameColor = sender?.let { MaterialTheme.messengerColors.getUserColor(sender.userId,messageBubbleStyle.color) } ?: Color.Unspecified
    val outlineColor = MaterialTheme.colorScheme.outline
    val isFocused = interactionSource.collectIsFocusedAsState()

    ThemedSurface(
        modifier = modifier,
        focused = isFocused.value,
        style = MaterialTheme.components.messageReference,
    ) {
        Column(
            modifier = Modifier
                .drawBehind {
                    drawRect(outlineColor, Offset.Zero, Size(5.dp.toPx(), size.height))
                }
                .padding(start = 10.dp, end = 5.dp, top = 5.dp, bottom = 5.dp)
        ) {
            Text(
                sender?.name ?: "",
                style = MaterialTheme.typography.labelLarge.copy(color = senderNameColor)
            )
            Spacer(Modifier.size(5.dp))
            content()
        }
    }
}
