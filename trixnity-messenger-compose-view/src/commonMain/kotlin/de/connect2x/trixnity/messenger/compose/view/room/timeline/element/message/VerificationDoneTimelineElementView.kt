package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.VerificationDoneTimelineElementViewModel
import kotlin.reflect.KClass

interface VerificationDoneMessageTimelineElementView : TimelineElementView<VerificationDoneTimelineElementViewModel>

class VerificationDoneMessageTimelineElementViewImpl : VerificationDoneMessageTimelineElementView {
    override val supports: KClass<out VerificationDoneTimelineElementViewModel> =
        VerificationDoneTimelineElementViewModel::class

    override suspend fun waitFor(element: VerificationDoneTimelineElementViewModel) {
        // NO-OP (has default size)
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: VerificationDoneTimelineElementViewModel,
        index: Int,
    ) {
        VerificationDoneElement(holder, element)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: VerificationDoneTimelineElementViewModel,
        index: Int,
    ) {
        VerificationDoneElement(holder, element)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: VerificationDoneTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: VerificationDoneTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: VerificationDoneTimelineElementViewModel
    ): ClipEntry? = null

    override fun a11yLabel(element: VerificationDoneTimelineElementViewModel, i18n: I18nView): String {
        return "${i18n.userVerificationStarted("")}, ${i18n.userVerificationDone()}, ${element.message}"
    }

}

@Composable
private fun VerificationDoneElement(
    holder: BaseTimelineElementHolderViewModel,
    element: VerificationDoneTimelineElementViewModel,
) {
    val i18n = DI.get<I18nView>()
    val isOwn = element.isOwn.collectAsState().value
    val sender = holder.sender.collectAsState().value

    if (isOwn == true) {
        ProvideTextStyle(TextStyle(fontSize = 12.sp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Shield, "")
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = i18n.userVerificationStarted(sender?.name ?: i18n.commonUnknown()),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.0f, fill = true).padding(end = 10.dp)
                        )
                        Icon(Icons.Default.SportsScore, i18n.userVerificationDone())
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            i18n.userVerificationSuccess(),
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(text = element.message)
                    }
                }
            }
        }
    }
}
