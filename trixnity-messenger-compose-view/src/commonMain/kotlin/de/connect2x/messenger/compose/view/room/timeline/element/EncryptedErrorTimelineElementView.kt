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
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubbleDisplayConfig
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubbleDisplayConfig.Companion.applyPreviewConfig
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EncryptedErrorTimelineElementViewModel
import kotlin.reflect.KClass


class EncryptedErrorTimelineElementView : TimelineElementView<EncryptedErrorTimelineElementViewModel> {
    override val supports: KClass<EncryptedErrorTimelineElementViewModel> =
        EncryptedErrorTimelineElementViewModel::class

    override suspend fun waitFor(element: EncryptedErrorTimelineElementViewModel) {
        // NO-OP (has default size)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: EncryptedErrorTimelineElementViewModel,
    ) {
        MessageBubble(
            holder = holder,
        ) { _ ->
            EncryptedMessageErrorElement()
        }
    }

    @Composable
    override fun createAsMessagePreview(
        holder: BaseTimelineElementHolderViewModel,
        element: EncryptedErrorTimelineElementViewModel,
        config: MessageBubbleDisplayConfig.() -> Unit,
    ) {
        MessageBubble(
            holder = holder,
            config = { applyPreviewConfig(config) },
        ) { _ ->
            EncryptedMessageErrorElement()
        }
    }

    @Composable
    override fun createReplyInTimeline(element: EncryptedErrorTimelineElementViewModel) {
        EncryptedMessageErrorElement()
    }

    @Composable
    override fun createReplyInSendMessage(element: EncryptedErrorTimelineElementViewModel) {
        EncryptedMessageErrorElement()
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
