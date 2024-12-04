package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReplyToViewModel

interface ReplyToAreaView {
    @Composable
    fun ColumnScope.create(replyToViewModel: ReplyToViewModel?)
}

@Composable
fun ColumnScope.ReplyToArea(replyToViewModel: ReplyToViewModel?) {
    with(DI.get<ReplyToAreaView>()) { create(replyToViewModel) }
}

class ReplyToAreaViewImpl : ReplyToAreaView {
    @Composable
    override fun ColumnScope.create(replyToViewModel: ReplyToViewModel?) {
        val i18n = DI.get<I18nView>()
        val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
        val replyTo = replyToViewModel?.replyTo?.collectAsState()?.value
        val isMobile = Platform.current.isMobile

        AnimatedVisibility(replyTo != null, enter = fadeIn() + slideInVertically(initialOffsetY = { 200 })) {
            Box {
                Row(Modifier.padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        i18n.replyTo(),
                        modifier = Modifier.padding(horizontal = if (isMobile) 10.dp else 15.dp),
                    )
                    // FIXME how to get a Holder here?
                    ReplyToPill(replyToViewModel!!) {
//                        timelineElementViewSelector.createReplyInSendMessage()
                    }
                }
            }
        }
    }
}

@Composable
fun ReplyToPill(replyToViewModel: ReplyToViewModel, content: @Composable () -> Unit) {
    val i18n = DI.get<I18nView>()
    val replyTo = replyToViewModel.replyTo.collectAsState().value

    ReferencedMessagePill(
        senderName = replyTo?.senderName ?: "",
        content = content,
    ) {
        IconButton(onClick = { replyToViewModel.cancelReplyTo() }, modifier = Modifier.buttonPointerModifier()) {
            Icon(Icons.Outlined.Cancel, i18n.replyToCancel())
        }
    }
}

@Composable
fun ReferencedMessagePill(
    senderName: String,
    content: @Composable () -> Unit,
    senderNameColor: Color = Color.Unspecified,
    suffix: @Composable (() -> Unit)? = null,
) {
    val fillMaxWidth = if (suffix == null) Modifier else Modifier.fillMaxWidth()
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .height(IntrinsicSize.Min)
            .then(fillMaxWidth)
    ) {
        Surface(color = Color(0x55FFFFFF)) { // We just want to have a slightly modified background
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.outline)
                )
                val weight = if (suffix == null) Modifier else Modifier.weight(1.0f, fill = true)
                Box(Modifier.padding(5.dp).then(weight)) {
                    Column {
                        Text(senderName, style = MaterialTheme.typography.labelLarge.copy(color = senderNameColor))
                        Spacer(Modifier.size(5.dp))
                        content()
                    }
                }
                suffix?.invoke()
            }
        }
    }
}
