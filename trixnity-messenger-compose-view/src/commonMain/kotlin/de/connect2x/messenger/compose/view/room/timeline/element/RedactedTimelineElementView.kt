package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubbleDisplayConfig
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubbleDisplayConfig.Companion.applyPreviewConfig
import de.connect2x.messenger.compose.view.theme.dp
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RedactedTimelineElementViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass


class RedactedTimelineElementView : TimelineElementView<RedactedTimelineElementViewModel> {
    override val supports: KClass<RedactedTimelineElementViewModel> = RedactedTimelineElementViewModel::class

    override suspend fun waitFor(element: RedactedTimelineElementViewModel) {
        element.message.filterNotNull().first()
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: RedactedTimelineElementViewModel,
    ) {
        MessageBubble(
            holder,
        ) {
            RedactedMessageElement(element)
        }
    }

    @Composable
    override fun createAsMessagePreview(
        holder: BaseTimelineElementHolderViewModel,
        element: RedactedTimelineElementViewModel,
        config: MessageBubbleDisplayConfig.() -> Unit,
    ) {
        MessageBubble(
            holder,
            config = { applyPreviewConfig(config) },
        ) {
            RedactedMessageElement(element)
        }
    }

    @Composable
    override fun createReplyInTimeline(element: RedactedTimelineElementViewModel) {
        RedactedMessageElement(element)
    }

    @Composable
    override fun createReplyInSendMessage(element: RedactedTimelineElementViewModel) {
        RedactedMessageElement(element)
    }
}

@Composable
internal fun RedactedMessageElement(element: RedactedTimelineElementViewModel) {
    val i18n = DI.get<I18nView>()
    val formattedMessage = element.message.collectAsState().value
    val redactedAt = element.redactedAt.collectAsState().value
    Row(Modifier.padding(10.dp)) {
        Icon(
            Icons.Outlined.Delete, i18n.commonDeleted(),
            Modifier.align(Alignment.CenterVertically)
                .size(MaterialTheme.typography.bodySmall.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "$formattedMessage${redactedAt.let { " ($it)" }}",
            Modifier.alignByBaseline(),
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
        )
    }
}
