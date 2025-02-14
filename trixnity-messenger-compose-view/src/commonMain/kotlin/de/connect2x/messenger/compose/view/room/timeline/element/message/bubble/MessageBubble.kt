package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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


data class MessageBubbleDisplayConfig(
    /**
     * Set this to `true` when the content has a determined height and is not a wrapped text or similar.
     */
    var contentNeedsMaxWidth: Boolean = false,

    /**
     * Whether the context menu should be available from the message bubble view.
     */
    var enableContextActionMenu: Boolean = true,

    /**
     * Whether the bubble should provide the ability to add reactions and display existing ones.
     */
    var showMessageReactions: Boolean = true,

    /**
     * Whether to render the message that the content of this message bubble has replied to.
     */
    var showRepliedElement: Boolean = true,

    /**
     * Whether the rendering of the chat bubble tail should be forced to be always rendered.
     */
    var alwaysShowChatBubbleTail: Boolean = false,

    /**
     * Whether the message bubble should use a reasonable maximum height when rendered.
     */
    var minifyBubble: Boolean = false,

    /**
     * Block click events on the message contents.
     */
    var preventUserInput: Boolean = false,

    ) {
    fun MessageBubbleDisplayConfig.applyPreviewConfig(
        additionalConfig: MessageBubbleDisplayConfig.() -> Unit = {},
    ) = this.apply {
        showMessageReactions = false
        enableContextActionMenu = false
        alwaysShowChatBubbleTail = true
        preventUserInput = true
        minifyBubble = true
        additionalConfig.invoke(this)
    }
}

interface MessageBubbleView {
    @Composable
    fun create(
        holder: BaseTimelineElementHolderViewModel,
        config: MessageBubbleDisplayConfig.() -> Unit = {},
        additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit = {},
        overlay: (@Composable BoxScope.() -> Unit)? = null,
        content: @Composable (onOpenActionMenu: () -> Unit) -> Unit,
    )
}

@Composable
fun MessageBubble(
    holder: BaseTimelineElementHolderViewModel,
    config: MessageBubbleDisplayConfig.() -> Unit = {},
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit = {},
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable (onOpenActionMenu: () -> Unit) -> Unit,
) {
    DI.get<MessageBubbleView>()
        .create(holder, config, additionalContextActions, overlay, content)
}

class MessageBubbleViewImpl : MessageBubbleView {
    @Composable
    override fun create(
        holder: BaseTimelineElementHolderViewModel,
        config: MessageBubbleDisplayConfig.() -> Unit,
        additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
        overlay: (@Composable BoxScope.() -> Unit)?,
        content: @Composable (onOpenActionMenu: () -> Unit) -> Unit,
    ) {
        val cfg = MessageBubbleDisplayConfig().apply(config)
        val isRedactionInProgress = holder.asTimelineElementHolder()
            ?.redactionInProgress?.collectAsState()?.value == true
        val showBigGap = holder.showBigGapBefore.collectAsState().value == true
        val showReactions = remember { mutableStateOf(false) }
        val topPadding = if (showBigGap) 10.dp else 3.dp

        BoxWithConstraints(
            Modifier.fillMaxWidth(),
        ) {
            val padding = isRedactionInProgress.let {
                (if (maxWidth < 400.dp) 20.dp else 80.dp) - (if (it) 16.dp else 0.dp)
            }
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
                    if (isRedactionInProgress) {
                        val i18n = DI.get<I18nView>()
                        Box(Modifier.size(16.dp).padding(2.dp)) {
                            Icon(Icons.Default.AutoDelete, i18n.messageBubbleBeingDeleted())
                        }
                    }
                    MessageBubbleContainer(
                        holder,
                        cfg,
                        showReactions,
                        additionalContextActions,
                        overlay,
                        content,
                    )
                }
                if (cfg.showMessageReactions) MessageReactions(
                    holder,
                    showReactions,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
