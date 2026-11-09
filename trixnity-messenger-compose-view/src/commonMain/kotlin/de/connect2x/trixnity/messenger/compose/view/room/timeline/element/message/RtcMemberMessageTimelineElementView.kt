package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.Indicator
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RtcMemberMessageTimelineElementViewModel
import kotlin.reflect.KClass

interface RtcMemberMessageTimelineElementView : TimelineElementView<RtcMemberMessageTimelineElementViewModel>

class RtcMemberMessageTimelineElementViewImpl : RtcMemberMessageTimelineElementView {
    override val supports: KClass<RtcMemberMessageTimelineElementViewModel> =
        RtcMemberMessageTimelineElementViewModel::class

    override suspend fun waitFor(element: RtcMemberMessageTimelineElementViewModel) {}

    override fun isFocusable(): Boolean {
        return true
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: RtcMemberMessageTimelineElementViewModel,
        index: Int,
    ) {
        GenericElement(holder, element)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: RtcMemberMessageTimelineElementViewModel,
        index: Int,
    ) {
        GenericElement(holder, element)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: RtcMemberMessageTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        GenericElement(holder, element)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: RtcMemberMessageTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        GenericElement(holder, element)
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: RtcMemberMessageTimelineElementViewModel,
    ): ClipEntry? {
        return null
    }

    override fun a11yLabel(element: RtcMemberMessageTimelineElementViewModel, i18n: I18nView): String? {
        return null // TODO
    }

    @OptIn(MSC4143::class)
    @Composable
    private fun GenericElement(
        holder: BaseTimelineElementHolderViewModel,
        element: RtcMemberMessageTimelineElementViewModel,
    ) {
        val i18n = DI.get<I18nView>()
        val sender = holder.sender.collectAsState().value?.name ?: i18n.commonUnknown()
        Indicator(MaterialTheme.colorScheme.tertiary, focusable = true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = i18n.callSymbol(),
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiary,
                    text = "$sender ${element.message}",
                )
            }
        }
    }
}
