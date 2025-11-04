package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.EncryptedErrorTimelineElementViewModel
import kotlin.reflect.KClass

interface EncryptedErrorTimelineElementView : TimelineElementView<EncryptedErrorTimelineElementViewModel>

class EncryptedErrorTimelineElementViewImpl : EncryptedErrorTimelineElementView {
    override val supports: KClass<EncryptedErrorTimelineElementViewModel> =
        EncryptedErrorTimelineElementViewModel::class

    override suspend fun waitFor(element: EncryptedErrorTimelineElementViewModel) {
        // no-op (has default size)
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: EncryptedErrorTimelineElementViewModel,
        index: Int,
    ) {
        MessageBubble(
            holder,
            needsMaxWidth = false,
            isPreview = false,
            index = index,
        ) { _ ->
            EncryptedMessageErrorElement()
        }
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: EncryptedErrorTimelineElementViewModel,
        index: Int,
    ) {
        MessageBubble(
            holder,
            needsMaxWidth = false,
            isPreview = true,
            index = index,
        ) { _ ->
            EncryptedMessageErrorElement()
        }
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: EncryptedErrorTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            modifier = modifier,
            interactionSource = interactionSource,
            content = {
                EncryptedMessageErrorElement()
            }
        )
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: EncryptedErrorTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            modifier = modifier,
            interactionSource = interactionSource,
            content = {
                EncryptedMessageErrorElement()
            }
        )
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: EncryptedErrorTimelineElementViewModel
    ): ClipEntry? = null

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
