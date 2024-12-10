package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RepliedTimelineElementHolderViewModel

@Composable
fun ReferencedMessagePill(
    repliedTimelineElementHolderViewModel: RepliedTimelineElementHolderViewModel,
    content: @Composable () -> Unit,
    suffix: @Composable (() -> Unit)? = null,
) {
    val sender = repliedTimelineElementHolderViewModel.sender.collectAsState().value
    val senderNameColor = sender?.let { MaterialTheme.messengerColors.getUserColor(sender.userId) } ?: Color.Unspecified
    val fillMaxWidth = if (suffix == null) Modifier else Modifier.fillMaxWidth()
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .height(IntrinsicSize.Min)
            .then(fillMaxWidth)
    ) {
        Surface(color = Color(0x55FFFFFF)) { // We just want to have a slightly modified background
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.outline)
                )
                val weight = if (suffix == null) Modifier else Modifier.weight(1.0f, fill = true)
                Box(Modifier.padding(5.dp).then(weight)) {
                    Column {
                        Text(
                            sender?.name ?: "",
                            style = MaterialTheme.typography.labelLarge.copy(color = senderNameColor)
                        )
                        Spacer(Modifier.size(5.dp))
                        content()
                    }
                }
                suffix?.invoke()
            }
        }
    }
}
