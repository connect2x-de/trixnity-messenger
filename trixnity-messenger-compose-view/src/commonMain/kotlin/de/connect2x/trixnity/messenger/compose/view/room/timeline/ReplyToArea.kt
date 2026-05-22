package de.connect2x.trixnity.messenger.compose.view.room.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.InputAreaViewModel

interface ReplyToAreaView {
    @Composable fun ColumnScope.create(inputAreaViewModel: InputAreaViewModel)
}

@Composable
fun ColumnScope.ReplyToArea(inputAreaViewModel: InputAreaViewModel) {
    with(DI.get<ReplyToAreaView>()) { create(inputAreaViewModel) }
}

class ReplyToAreaViewImpl : ReplyToAreaView {
    @Composable
    override fun ColumnScope.create(inputAreaViewModel: InputAreaViewModel) {
        val i18n = DI.get<I18nView>()
        val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
        val repliedElementHolder = inputAreaViewModel.repliedElement.collectAsState().value
        val element = repliedElementHolder?.element?.collectAsState()?.value

        AnimatedVisibility(element != null, enter = fadeIn() + slideInVertically(initialOffsetY = { 200 })) {
            Box {
                Row(
                    Modifier.padding(top = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        i18n.replyTo(),
                        modifier = Modifier.padding(start = 14.dp, end = 10.dp),
                    )

                    Box(Modifier.weight(1f, fill = true)) {
                        element?.let {
                            timelineElementViewSelector.createReplyInSendMessage(repliedElementHolder, element)
                        }
                    }

                    Tooltip(tooltip = { Text(i18n.replyToCancel()) }) {
                        ThemedIconButton(
                            style = MaterialTheme.components.commonIconButton,
                            onClick = { inputAreaViewModel.cancelReply() },
                        ) {
                            Icon(Icons.Outlined.Cancel, i18n.replyToCancel())
                        }
                    }
                }
            }
        }
    }
}
