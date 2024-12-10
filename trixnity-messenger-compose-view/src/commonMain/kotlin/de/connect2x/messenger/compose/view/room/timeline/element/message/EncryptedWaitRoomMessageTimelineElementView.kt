package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.messenger.compose.view.theme.dp
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EncryptedWaitTimelineElementViewModel
import kotlin.reflect.KClass

class EncryptedWaitRoomMessageTimelineElementView : TimelineElementView<EncryptedWaitTimelineElementViewModel> {
    override val supports: KClass<EncryptedWaitTimelineElementViewModel> = EncryptedWaitTimelineElementViewModel::class

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: EncryptedWaitTimelineElementViewModel
    ) {
        MessageBubble(
            holder,
            needsMaxWidth = false,
        ) { _ ->
            EncryptedMessage()
        }
    }

    @Composable
    override fun createReplyInTimeline(element: EncryptedWaitTimelineElementViewModel) {
        EncryptedMessage()
    }

    @Composable
    override fun createReplyInSendMessage(element: EncryptedWaitTimelineElementViewModel) {
        EncryptedMessage()
    }
}

@Composable
internal fun EncryptedMessage() {
    val i18n = DI.get<I18nView>()
    Row(Modifier.padding(10.dp)) {
        Icon(
            Icons.Outlined.Lock, i18n.commonWaiting(),
            Modifier.align(Alignment.CenterVertically)
                .size(MaterialTheme.typography.bodySmall.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            i18n.messageContentWaitForKeys(),
            Modifier.alignByBaseline(),
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
        )
    }
}
