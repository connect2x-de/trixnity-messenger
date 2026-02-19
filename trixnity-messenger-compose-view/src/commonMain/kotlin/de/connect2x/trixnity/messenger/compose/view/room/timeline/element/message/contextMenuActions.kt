package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboard
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.util.asOutboxElementHolder
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.util.asTimelineElementHolder
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedActionMenuItem
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import kotlinx.coroutines.launch

@Composable
internal fun BaseTimelineElementHolderViewModel.contextMenuActions(
    i18n: I18nView,
    onOpenMetadata: () -> Unit,
    onReactToMessage: () -> Unit,
    onRedact: () -> Unit,
): List<BaseTimelineElementHolderContextMenuAction> {
    val canBeReactedTo = asTimelineElementHolder()?.canBeReactedTo?.collectAsState()?.value == true
    val canBeRepliedTo = asTimelineElementHolder()?.canBeRepliedTo?.collectAsState()?.value == true
    val canBeEdited = asTimelineElementHolder()?.canBeEdited?.collectAsState()?.value == true
    val canBeRedacted = asTimelineElementHolder()?.canBeRedacted?.collectAsState()?.value == true
    val canBeReported = asTimelineElementHolder()?.canBeReported?.collectAsState()?.value == true
    val canRetrySend = asOutboxElementHolder()?.canRetrySend?.collectAsState()?.value == true
    val canAbortSend = asOutboxElementHolder()?.canAbortSend?.collectAsState()?.value == true

    val clipboard = LocalClipboard.current
    val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
    val clipEntry =
        this.element.collectAsState().value?.let { timelineElementViewModel ->
            timelineElementViewSelector.getClipEntry(this, timelineElementViewModel)
        }
    val canBeCopied = clipEntry != null
    val coroutineScope = rememberCoroutineScope()

    return buildList {
        if (this@contextMenuActions is TimelineElementHolderViewModel) {
            add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.infoMessage(),
                    action = onOpenMetadata,
                )
            )
            if (canBeReactedTo) add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.reactMessage(),
                    action = onReactToMessage,
                )
            )
            if (canBeRepliedTo) add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.replyMessage(),
                    action = ::reply,
                )
            )
            if (canBeEdited) add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.editMessage(),
                    action = ::replace,
                )
            )
            if (canBeRedacted) add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.redactMessage(),
                    action = onRedact,
                )
            )
            if (canBeReported) add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.reportMessage(),
                    action = ::report,
                )
            )
            if (canBeCopied) add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.commonCopy(),
                    action = {
                        coroutineScope.launch {
                            clipboard.setClipEntry(clipEntry)
                        }
                    },
                )
            )
        }
        if (this@contextMenuActions is OutboxElementHolderViewModel) {
            if (canRetrySend) add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.retrySendMessage(),
                    action = ::retrySend,
                )
            )
            if (canAbortSend) add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.abortSendMessage(),
                    action = ::abortSend,
                )
            )
        }
    }
}

class BaseTimelineElementHolderContextMenuAction(
    override val label: String,
    override val isEnabled: Boolean = true,
    override val action: () -> Unit,
) : ThemedActionMenuItem(label, isEnabled, action)
