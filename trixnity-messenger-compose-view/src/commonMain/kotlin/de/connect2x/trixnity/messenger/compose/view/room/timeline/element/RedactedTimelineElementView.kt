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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.Indicator
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.compose.view.theme.dp
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RedactedMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RedactedStateTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RedactedTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

interface RedactedMessageTimelineElementView : TimelineElementView<RedactedMessageTimelineElementViewModel>

interface RedactedStateTimelineElementView : TimelineElementView<RedactedStateTimelineElementViewModel>

class RedactedMessageTimelineElementViewImpl : RedactedMessageTimelineElementView {
    override val supports: KClass<RedactedMessageTimelineElementViewModel> =
        RedactedMessageTimelineElementViewModel::class

    override suspend fun waitFor(element: RedactedMessageTimelineElementViewModel) {
        element.message.filterNotNull().first()
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: RedactedMessageTimelineElementViewModel,
        index: Int,
    ) {
        MessageBubble(holder, needsMaxWidth = false, isPreview = false, isMentioned = false, index = index) { _ ->
            RedactedMessageElement(element, 10.dp)
        }
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: RedactedMessageTimelineElementViewModel,
        index: Int,
    ) {
        MessageBubble(holder, needsMaxWidth = false, isPreview = true, isMentioned = false, index = index) { _ ->
            RedactedMessageElement(element, 10.dp)
        }
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: RedactedMessageTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            element = element,
            modifier = modifier,
            interactionSource = interactionSource,
            content = { RedactedMessageElement(element, 10.dp) },
        )
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: RedactedMessageTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            element = element,
            modifier = modifier,
            interactionSource = interactionSource,
            content = { RedactedMessageElement(element, 10.dp) },
        )
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: RedactedMessageTimelineElementViewModel,
    ): ClipEntry? = null
}

class RedactedStateTimelineElementViewImpl : RedactedStateTimelineElementView {
    override val supports: KClass<RedactedStateTimelineElementViewModel> = RedactedStateTimelineElementViewModel::class

    override suspend fun waitFor(element: RedactedStateTimelineElementViewModel) {
        element.message.filterNotNull().first()
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: RedactedStateTimelineElementViewModel,
        index: Int,
    ) {
        Indicator(MaterialTheme.colorScheme.tertiary, focusable = true) { RedactedMessageElement(element) }
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: RedactedStateTimelineElementViewModel,
        index: Int,
    ) {
        Indicator(MaterialTheme.colorScheme.tertiary, focusable = true) { RedactedMessageElement(element) }
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: RedactedStateTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            element = element,
            modifier = modifier,
            interactionSource = interactionSource,
            content = { RedactedMessageElement(element) },
        )
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: RedactedStateTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            element = element,
            modifier = modifier,
            interactionSource = interactionSource,
            content = { RedactedMessageElement(element) },
        )
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: RedactedStateTimelineElementViewModel,
    ): ClipEntry? = null
}

@Composable
internal fun RedactedMessageElement(element: RedactedTimelineElementViewModel, padding: Dp? = null) {
    val i18n = DI.get<I18nView>()
    val formattedMessage = element.message.collectAsState().value
    val redactedAt = element.redactedAt.collectAsState().value
    val reason = element.reason.collectAsState().value
    val modifier = if (padding != null) Modifier.padding(padding) else Modifier
    Row(modifier) {
        Icon(
            Icons.Outlined.Delete,
            i18n.commonDeleted(),
            Modifier.align(Alignment.CenterVertically).size(MaterialTheme.typography.bodySmall.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "${formattedMessage?:""}${redactedAt?.let { " ($it)" }?:""}${reason?.let { ": $it" }?:""}",
            Modifier.alignByBaseline(),
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
        )
    }
}
