package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.MessageInfo
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
    var showContextActionMenu: Boolean = true,

    /**
     * Whether the bubble should provide the ability to add reactions and display existing ones.
     */
    var showMessageReactions: Boolean = true,

    /**
     * Whether to render the message that the content of this message bubble has replied to.
     */
    var showRepliedElement: Boolean = true,

    /**
     * Whether the bubble should render the timestamp and message has been edited indicator.
     */
    var showTimeAndEditedIndicator: Boolean = true,

    /**
     * Whether the rendering of the chat bubble tail should be forced to be always rendered.
     */
    var alwaysShowChatBubbleTail: Boolean = false,

    /**
     * Provide the padding from the message bubble to its container.
     */
    var bubblePadding: BoxWithConstraintsScope.(redactionInProgress: Boolean) -> Dp = {
        (if (maxWidth < 400.dp) 20.dp else 80.dp) - (if (it) 16.dp else 0.dp)
    },

    ) {
    companion object {
        fun of(config: MessageBubbleDisplayConfig.() -> Unit) =
            MessageBubbleDisplayConfig().apply(config).copy()

        fun MessageBubbleDisplayConfig.applyPreviewConfig() = this.apply {
            showMessageReactions = false
            showContextActionMenu = false
            showTimeAndEditedIndicator = false
            alwaysShowChatBubbleTail = true
            bubblePadding = { _ -> 0.dp }
        }
    }
}

interface MessageBubbleView {
    @Composable
    fun create(
        holder: BaseTimelineElementHolderViewModel,
        config: MessageBubbleDisplayConfig.() -> Unit = {},
        additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit = {},
        overlay: (@Composable BoxScope.() -> Unit)? = null,
        content: @Composable (showActionMenu: () -> Unit) -> Unit,
    )
}

@Composable
fun MessageBubble(
    holder: BaseTimelineElementHolderViewModel,
    config: MessageBubbleDisplayConfig.() -> Unit = {},
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit = {},
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable (showActionMenu: () -> Unit) -> Unit,
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
        content: @Composable (showActionMenu: () -> Unit) -> Unit,
    ) {
        val cfg = MessageBubbleDisplayConfig.of(config)
        val redactionInProgress =
            holder.asTimelineElementHolder()?.redactionInProgress?.collectAsState()?.value == true
        val showBigGap = holder.showBigGapBefore.collectAsState().value == true
        val topPadding = if (showBigGap) 10.dp else 3.dp

        val infoOpen = remember { mutableStateOf(false) }
        val reactionsOpen = remember { mutableStateOf(false) }

        BoxWithConstraints(
            Modifier.fillMaxWidth(),
        ) {
            val padding = cfg.bubblePadding(this@BoxWithConstraints, redactionInProgress)
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
                        holder,
                        cfg,
                        infoOpen,
                        reactionsOpen,
                        additionalContextActions,
                        overlay,
                        content,
                    )
                }

                // TODO: remove
                MessageInfo(
                    holder,
                    infoOpen,
                    modifier = Modifier.padding(start = 8.dp),
                )

                if (cfg.showMessageReactions) MessageReactions(
                    holder,
                    reactionsOpen,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
