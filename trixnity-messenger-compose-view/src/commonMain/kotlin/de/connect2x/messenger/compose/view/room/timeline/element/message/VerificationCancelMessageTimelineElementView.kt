package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.MessageTimelineElementViewModel.VerificationCancel
import kotlin.reflect.KClass

class VerificationCancelMessageTimelineElementView : TimelineElementView<VerificationCancel> {
    override val supports: KClass<out VerificationCancel> =
        VerificationCancel::class

    override suspend fun waitFor(element: VerificationCancel) {
        // NO-OP (has default size)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: VerificationCancel
    ) {
        VerificationCancelElement(holder, element)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: VerificationCancel
    ) {
        VerificationCancelElement(holder, element)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: VerificationCancel
    ) {
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: VerificationCancel
    ) {
    }

}

@Composable
private fun VerificationCancelElement(
    holder: BaseTimelineElementHolderViewModel,
    element: VerificationCancel,
) {
    val i18n = DI.get<I18nView>()
    val sender = holder.sender.collectAsState().value
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
                        Icons.Default.Cancel,
                        i18n.userVerificationNotSuccessful(),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(text = element.cause)
                }
            }
        }
    }
}
