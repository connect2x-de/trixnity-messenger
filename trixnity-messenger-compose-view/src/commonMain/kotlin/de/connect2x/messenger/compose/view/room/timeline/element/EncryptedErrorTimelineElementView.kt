package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.MessageTimelineElementViewModel
import kotlin.reflect.KClass

class EncryptedErrorTimelineElementView : TimelineElementView<MessageTimelineElementViewModel.EncryptedError> {
    override val supports: KClass<MessageTimelineElementViewModel.EncryptedError> =
        MessageTimelineElementViewModel.EncryptedError::class

    override suspend fun waitFor(element: MessageTimelineElementViewModel.EncryptedError) {
        // no-op (has default size)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: MessageTimelineElementViewModel.EncryptedError,
    ) {
        MessageBubble(
            holder,
            needsMaxWidth = false,
            isPreview = false,
        ) { _ ->
            EncryptedMessageErrorElement()
        }
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: MessageTimelineElementViewModel.EncryptedError,
    ) {
        MessageBubble(
            holder,
            needsMaxWidth = false,
            isPreview = true,
        ) { _ ->
            EncryptedMessageErrorElement()
        }
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: MessageTimelineElementViewModel.EncryptedError,
    ) {
        ReferencedMessagePill(
            holder = holder,
            content = {
                EncryptedMessageErrorElement()
            }
        )
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: MessageTimelineElementViewModel.EncryptedError) {
        ReferencedMessagePill(
            holder = holder,
            content = {
                EncryptedMessageErrorElement()
            }
        )
    }

}

@Composable
internal fun EncryptedMessageErrorElement() {
    val i18n = DI.get<I18nView>()
    Text(
        i18n.messageContentNoDecryption(),
        Modifier.padding(10.dp),
        style = MaterialTheme.typography.bodySmall,
        fontStyle = FontStyle.Italic,
    )
}
