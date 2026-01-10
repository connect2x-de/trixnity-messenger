package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.MessageReactions
import de.connect2x.messenger.compose.view.room.timeline.element.util.asTimelineElementHolder
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel

interface MessageBubbleView {
    @Composable
    fun create(
        holder: BaseTimelineElementHolderViewModel,
        needsMaxWidth: Boolean,
        additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit = {},
        isPreview: Boolean,
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
    index: Int,
    content: @Composable (showActionMenu: () -> Unit) -> Unit,
) {
    DI.get<MessageBubbleView>()
        .create(holder, needsMaxWidth, additionalContextActions, isPreview, index, content)
}

class MessageBubbleViewImpl : MessageBubbleView {
    @Composable
    override fun create(
        holder: BaseTimelineElementHolderViewModel,
        needsMaxWidth: Boolean,
        additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
        isPreview: Boolean,
        index: Int,
        content: @Composable (showActionMenu: () -> Unit) -> Unit,
    ) {
        val redactionInProgress =
            holder.asTimelineElementHolder()?.redactionInProgress?.collectAsState()?.value == true
        val showBigGap = holder.showBigGapBefore.collectAsState().value == true
        val topPadding = if (showBigGap) 10.dp else 3.dp
        val showRedactWarning = remember { mutableStateOf(false) }

        val reactionsOpen = remember { mutableStateOf(false) }

        val interactionSource = remember { MutableInteractionSource() }

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
                        interactionSource = interactionSource,
                        index = index,
                        onRedact = {
                            println("Showing redact warning")
                            showRedactWarning.value = true
                        },
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
        val i18n = DI.get<I18nView>()
        val holder = holder.asTimelineElementHolder()
        if (showRedactWarning.value) {
            ThemedModalDialog(onDismissRequest = { showRedactWarning.value = false }) {
                ModalDialogHeader { Text(i18n.redactionWarningInfoTitle()) }
                ModalDialogContent {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, i18n.commonWarning(), tint = MaterialTheme.messengerColors.warning)
                        SmallSpacer()
                        Text(i18n.redactionWarningInfo())
                    }
                }
                ModalDialogFooter {
                    ThemedButton(
                        onClick = { showRedactWarning.value = false },
                        style = MaterialTheme.components.commonButton
                    ) {
                        Text(i18n.commonCancel())
                    }
                    ThemedButton(onClick = { holder?.redact() }, style = MaterialTheme.components.primaryButton) {
                        Text(i18n.commonConfirm())
                    }
                }
            }
        }
    }
}
