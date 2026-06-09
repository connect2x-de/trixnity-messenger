package de.connect2x.trixnity.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.compose.view.theme.dp
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RedactedTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

interface RedactedTimelineElementView : TimelineElementView<RedactedTimelineElementViewModel>

class RedactedTimelineElementViewImpl : RedactedTimelineElementView {
    override val supports: KClass<RedactedTimelineElementViewModel> = RedactedTimelineElementViewModel::class

    override suspend fun waitFor(element: RedactedTimelineElementViewModel) {
        element.message.filterNotNull().first()
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: RedactedTimelineElementViewModel,
        index: Int,
    ) {
        MessageBubble(holder, needsMaxWidth = false, isPreview = false, index = index) { _ ->
            RedactedMessageElement(element)
        }
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: RedactedTimelineElementViewModel,
        index: Int,
    ) {
        MessageBubble(holder, needsMaxWidth = false, isPreview = true, index = index) { _ ->
            RedactedMessageElement(element)
        }
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: RedactedTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            modifier = modifier,
            interactionSource = interactionSource,
            content = { RedactedMessageElement(element) },
        )
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: RedactedTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            modifier = modifier,
            interactionSource = interactionSource,
            content = { RedactedMessageElement(element) },
        )
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: RedactedTimelineElementViewModel,
    ): ClipEntry? = null
}

@Composable
internal fun RedactedMessageElement(element: RedactedTimelineElementViewModel) {
    val i18n = DI.get<I18nView>()
    val formattedMessage = element.message.collectAsState().value
    val redactedAt = element.redactedAt.collectAsState().value
    val reason = element.reason.collectAsState().value
    Row(Modifier.padding(10.dp)) {
        Icon(
            Icons.Outlined.Delete,
            i18n.commonDeleted(),
            Modifier.align(Alignment.CenterVertically).size(MaterialTheme.typography.bodySmall.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "$formattedMessage${redactedAt?.let { " ($it)" }?:""}${reason?.let { ": $it" }?:""}",
            Modifier.alignByBaseline(),
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
        )
    }
}
