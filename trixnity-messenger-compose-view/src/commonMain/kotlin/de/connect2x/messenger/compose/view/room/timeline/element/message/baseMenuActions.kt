package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.messenger.compose.view.room.timeline.element.util.asOutboxElementHolder
import de.connect2x.messenger.compose.view.room.timeline.element.util.asTimelineElementHolder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel

class BaseTimelineElementHolderContextMenuAction(
    val label: String,
    internal val action: () -> Unit
) {
    operator fun invoke() = action()
}

@Composable
internal fun BaseTimelineElementHolderViewModel.baseMenuActions(
    i18n: I18nView,
    onInfo: () -> Unit,
    onInfoLegacy: () -> Unit, // TODO remove
    onReact: () -> Unit,
): List<BaseTimelineElementHolderContextMenuAction> {
    val canBeReactedTo = asTimelineElementHolder()?.canBeReactedTo?.collectAsState()?.value == true
    val canBeRepliedTo = asTimelineElementHolder()?.canBeRepliedTo?.collectAsState()?.value == true
    val canBeEdited = asTimelineElementHolder()?.canBeEdited?.collectAsState()?.value == true
    val canBeRedacted = asTimelineElementHolder()?.canBeRedacted?.collectAsState()?.value == true
    val canBeReportedTo = asTimelineElementHolder()?.canBeReported?.collectAsState()?.value == true
    val canRetrySend = asOutboxElementHolder()?.canRetrySend?.collectAsState()?.value == true
    val canAbortSend = asOutboxElementHolder()?.canAbortSend?.collectAsState()?.value == true
    return buildList {
        if (this@baseMenuActions is TimelineElementHolderViewModel) {
            add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.infoMessage(),
                    action = onInfo,
                )
            )
        }
        if (this@baseMenuActions is TimelineElementHolderViewModel) {
            add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.infoMessage() + " LEGACY",
                    action = onInfoLegacy,
                )
            )
        }
        if (this@baseMenuActions is TimelineElementHolderViewModel && canBeReactedTo) {
            add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.reactMessage(),
                    action = onReact,
                )
            )
        }
        if (this@baseMenuActions is TimelineElementHolderViewModel && canBeRepliedTo) {
            add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.replyMessage(),
                    action = ::reply,
                )
            )
        }
        if (this@baseMenuActions is TimelineElementHolderViewModel && canBeEdited) {
            add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.editMessage(),
                    action = ::replace,
                )
            )
        }
        if (this@baseMenuActions is TimelineElementHolderViewModel && canBeRedacted) {
            add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.redactMessage(),
                    action = ::redact,
                )
            )
        }
        if (this@baseMenuActions is TimelineElementHolderViewModel && canBeReportedTo) {
            add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.reportMessage(),
                    action = ::report,
                )
            )
        }
        if (this@baseMenuActions is OutboxElementHolderViewModel && canRetrySend) {
            add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.retrySendMessage(),
                    action = ::retrySend,
                )
            )
        }
        if (this@baseMenuActions is OutboxElementHolderViewModel && canAbortSend) {
            add(
                BaseTimelineElementHolderContextMenuAction(
                    label = i18n.abortSendMessage(),
                    action = ::abortSend,
                )
            )
        }
    }
}

@Composable
fun BaseTimelineElementHolderContextMenuAction.render(onClose: () -> Unit) {
    if (Platform.current.isMobile) {
        bottomSheetItem(onClose)
    } else {
        dropDownMenuItem(onClose)
    }
}

@Composable
internal fun BaseTimelineElementHolderContextMenuAction.dropDownMenuItem(
    onClose: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                label,
                Modifier.buttonPointerModifier(),
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        onClick = {
            onClose()
            action()
        },
        contentPadding = PaddingValues(horizontal = 10.dp),
    )
}

@Composable
internal fun BaseTimelineElementHolderContextMenuAction.bottomSheetItem(
    onClose: () -> Unit,
) {
    Text(
        label,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()
            .clickable {
                action()
                onClose()
            },
    )
}
