package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.room.timeline.RedactionWarning
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.MessageReactions
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.util.asTimelineElementHolder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel

interface MessageBubbleView {
    @Composable
    fun create(
        holder: BaseTimelineElementHolderViewModel,
        needsMaxWidth: Boolean,
        additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
        isPreview: Boolean,
        isMentioned: Boolean,
        index: Int,
        content: @Composable (showActionMenu: () -> Unit) -> Unit,
    )
}

@Composable
fun MessageBubble(
    holder: BaseTimelineElementHolderViewModel,
    needsMaxWidth: Boolean,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit = {},
    isPreview: Boolean,
    isMentioned: Boolean,
    index: Int,
    content: @Composable (showActionMenu: () -> Unit) -> Unit,
) {
    DI.get<MessageBubbleView>()
        .create(holder, needsMaxWidth, additionalContextActions, isPreview, isMentioned, index, content)
}

class MessageBubbleViewImpl : MessageBubbleView {
    @Composable
    override fun create(
        holder: BaseTimelineElementHolderViewModel,
        needsMaxWidth: Boolean,
        additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
        isPreview: Boolean,
        isMentioned: Boolean,
        index: Int,
        content: @Composable (showActionMenu: () -> Unit) -> Unit,
    ) {
        val timelineElementHolder = holder.asTimelineElementHolder()
        val showBigGap = holder.showBigGapBefore.collectAsState().value == true
        val topPadding = if (showBigGap) 10.dp else 3.dp
        val showRedactionWarning = timelineElementHolder?.showRedactionWarning?.collectAsState()?.value == true

        val reactionsOpen = remember { mutableStateOf(false) }

        val interactionSource = remember { MutableInteractionSource() }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val padding = (if (maxWidth < 400.dp) 20.dp else 80.dp)
            Column(
                modifier =
                    Modifier.run {
                        if (holder.isByMe) padding(start = padding, top = topPadding).align(Alignment.CenterEnd)
                        else padding(end = padding, top = topPadding)
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = if (holder.isByMe) Alignment.End else Alignment.Start,
            ) {
                Row {
                    MessageBubbleContainer(
                        holder = holder,
                        needsMaxWidth = needsMaxWidth,
                        reactionsOpen = reactionsOpen,
                        additionalContextActions = additionalContextActions,
                        isPreview = isPreview,
                        isMentioned = isMentioned,
                        interactionSource = interactionSource,
                        index = index,
                        onRedact = { timelineElementHolder?.redact() },
                        content = content,
                    )
                }
                if (isPreview.not()) {
                    MessageReactions(holder, reactionsOpen, modifier = Modifier.padding(start = 8.dp))
                    if (showRedactionWarning) {
                        RedactionWarning(holder)
                    }
                }
            }
        }
    }
}
