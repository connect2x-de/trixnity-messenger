package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.compose.view.theme.dp
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.EncryptedWaitTimelineElementViewModel
import kotlin.reflect.KClass

interface EncryptedWaitTimelineElementView : TimelineElementView<EncryptedWaitTimelineElementViewModel>

class EncryptedWaitTimelineElementViewImpl : EncryptedWaitTimelineElementView {
    override val supports: KClass<EncryptedWaitTimelineElementViewModel> = EncryptedWaitTimelineElementViewModel::class

    override suspend fun waitFor(element: EncryptedWaitTimelineElementViewModel) {
        // no-op (has default size)
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: EncryptedWaitTimelineElementViewModel,
        index: Int,
    ) {
        MessageBubble(
            holder,
            needsMaxWidth = false,
            isPreview = false,
            isMentioned = false,
            index = index,
        ) { _ ->
            EncryptedMessageWaitElement()
        }
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: EncryptedWaitTimelineElementViewModel,
        index: Int,
    ) {
        MessageBubble(holder, needsMaxWidth = false, isPreview = true, isMentioned = false, index = index) { _ ->
            EncryptedMessageWaitElement()
        }
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: EncryptedWaitTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            modifier = modifier,
            interactionSource = interactionSource,
            content = { EncryptedMessageWaitElement() },
        )
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: EncryptedWaitTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            modifier = modifier,
            interactionSource = interactionSource,
            content = { EncryptedMessageWaitElement() },
        )
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: EncryptedWaitTimelineElementViewModel,
    ): ClipEntry? = null

    override fun a11yLabel(element: EncryptedWaitTimelineElementViewModel, i18n: I18nView): String {
        return i18n.messageContentWaitForKeys()
    }
}

@Composable
internal fun EncryptedMessageWaitElement() {
    val i18n = DI.get<I18nView>()
    Row(Modifier.padding(10.dp)) {
        Icon(
            Icons.Outlined.Lock,
            i18n.commonWaiting(),
            Modifier.align(Alignment.CenterVertically).size(MaterialTheme.typography.bodySmall.dp),
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
