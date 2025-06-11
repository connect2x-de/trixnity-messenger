package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.ReadMarker
import de.connect2x.messenger.compose.view.room.timeline.element.util.asOutboxElementHolder
import de.connect2x.messenger.compose.view.room.timeline.element.util.asTimelineElementHolder
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel

@Composable
fun MessageBubbleContent(
    holder: BaseTimelineElementHolderViewModel,
    needsMaxWidth: Boolean,
    showActionMenu: () -> Unit,
    content: @Composable (showActionMenu: () -> Unit) -> Unit,
) {
    val highlight = holder.asTimelineElementHolder()?.highlight?.collectAsState()?.value == true
    val sendError = holder.asOutboxElementHolder()?.sendError?.collectAsState()?.value
    val showSender = holder.showSender.collectAsState().value == true
    val isReplaced = holder.asTimelineElementHolder()?.isReplaced?.collectAsState()?.value == true
    val hasRepliedElement = holder.isReply.collectAsState().value == true

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
                .then(highlighted)
        ) {
            if (showSender) {
                Box(
                    Modifier
                        .padding(start = 10.dp, end = 10.dp, top = 5.dp)
                ) {
                    val sender = holder.sender.collectAsState().value
                    if (sender == null) {
                        // TODO placeholder instead
                        ThemedProgressIndicator(style = MaterialTheme.components.extraSmallCircularProgressIndicator)
                    } else {
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
            }

            RepliedElement(holder)

            // the hasRepliedElement is needed to avoid layouting already layouted elements which leads to this: "Asking for intrinsic measurements of SubcomposeLayout layouts is not supported."
            content(showActionMenu)
            Row(
                Modifier.align(Alignment.End).padding(5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                MessageBubbleContentInfo(isReplaced, holder)
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

@Composable
private fun MessageBubbleContentInfo(isReplaced: Boolean, holder: BaseTimelineElementHolderViewModel) {
    val i18n = DI.get<I18nView>()

    Row {
        if (isReplaced) {
            Text(
                i18n.messageBubbleEdited(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.paddingFromBaseline(0.dp)
                    .padding(end = 2.dp),
                maxLines = 1,
            )
        }
        Box(
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                holder.formattedTime,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.paddingFromBaseline(0.dp),
                maxLines = 1,
            )
        }
        ReadMarker(holder)
    }
}
