package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.MessageReactions
import de.connect2x.messenger.compose.view.room.timeline.element.util.asTimelineElementHolder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel

interface MessageBubbleView {
    @Composable
    fun create(
        holder: BaseTimelineElementHolderViewModel,
        needsMaxWidth: Boolean,
        additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit = {},
        isPreview: Boolean,
        content: @Composable (showActionMenu: () -> Unit) -> Unit,
    )
}

@Composable
fun MessageBubble(
    holder: BaseTimelineElementHolderViewModel,
    needsMaxWidth: Boolean,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit = {},
    isPreview: Boolean,
    content: @Composable (showActionMenu: () -> Unit) -> Unit,
) {
    DI.get<MessageBubbleView>()
        .create(holder, needsMaxWidth, additionalContextActions, isPreview, content)
}

class MessageBubbleViewImpl : MessageBubbleView {
    @Composable
    override fun create(
        holder: BaseTimelineElementHolderViewModel,
        needsMaxWidth: Boolean,
        additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
        isPreview: Boolean,
        content: @Composable (showActionMenu: () -> Unit) -> Unit,
    ) {
        val redactionInProgress =
            holder.asTimelineElementHolder()?.redactionInProgress?.collectAsState()?.value == true
        val showBigGap = holder.showBigGapBefore.collectAsState().value == true
        val topPadding = if (showBigGap) 10.dp else 3.dp

        val reactionsOpen = remember { mutableStateOf(false) }

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
                        holder = holder,
                        needsMaxWidth = needsMaxWidth,
                        reactionsOpen = reactionsOpen,
                        additionalContextActions = additionalContextActions,
                        isPreview = isPreview,
                        content = content,
                    )
                }
                if (isPreview.not()) {
                    MessageReactions(
                        holder,
                        reactionsOpen,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
