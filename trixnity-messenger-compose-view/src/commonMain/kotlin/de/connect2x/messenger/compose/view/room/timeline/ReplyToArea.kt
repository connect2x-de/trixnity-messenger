package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.InputAreaViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RepliedTimelineElementHolderViewModel

interface ReplyToAreaView {
    @Composable
    fun ColumnScope.create(inputAreaViewModel: InputAreaViewModel)
}

@Composable
fun ColumnScope.ReplyToArea(inputAreaViewModel: InputAreaViewModel) {
    with(DI.get<ReplyToAreaView>()) { create(inputAreaViewModel) }
}

class ReplyToAreaViewImpl : ReplyToAreaView {
    @Composable
    override fun ColumnScope.create(
        inputAreaViewModel: InputAreaViewModel,
    ) {
        val i18n = DI.get<I18nView>()
        val isMobile = Platform.current.isMobile
        val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
        val repliedElement = inputAreaViewModel.repliedElement.collectAsState().value
        val element = repliedElement?.element?.collectAsState()?.value

        AnimatedVisibility(element != null, enter = fadeIn() + slideInVertically(initialOffsetY = { 200 })) {
            Box {
                Row(Modifier.padding(top = 7.dp, end = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        i18n.replyTo(),
                        modifier = Modifier.padding(horizontal = if (isMobile) 10.dp else 15.dp),
                    )
                    element?.let {
                        ReplyToPill(inputAreaViewModel, repliedElement) {
                            timelineElementViewSelector.createReplyInSendMessage(element)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReplyToPill(
    inputAreaViewModel: InputAreaViewModel,
    repliedTimelineElementViewModel: RepliedTimelineElementHolderViewModel,
    content: @Composable (() -> Unit),
) {
    val i18n = DI.get<I18nView>()

    ReferencedMessagePill(
        repliedTimelineElementHolderViewModel = repliedTimelineElementViewModel,
        content = content,
    ) {
        IconButton(onClick = { inputAreaViewModel.cancelReply() }, modifier = Modifier.buttonPointerModifier()) {
            Icon(Icons.Outlined.Cancel, i18n.replyToCancel())
        }
    }
}

