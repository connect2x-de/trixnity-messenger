package de.connect2x.messenger.compose.view.room.timeline.element.message

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
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EncryptedErrorTimelineElementViewModel
import kotlin.reflect.KClass

class EncryptedErrorRoomMessageTimelineElementView : TimelineElementView<EncryptedErrorTimelineElementViewModel> {
    override val supports: KClass<EncryptedErrorTimelineElementViewModel> =
        EncryptedErrorTimelineElementViewModel::class

    @Composable
    override fun create(
        holder: BaseTimelineElementHolderViewModel,
        element: EncryptedErrorTimelineElementViewModel
    ) {
        MessageBubble(
            holder,
            element,
            showDate = true,
            needsMaxWidth = false,
        ) { _ ->
            EncryptedError()
        }
    }

}

@Composable
private fun EncryptedError() {
    val i18n = DI.get<I18nView>()
    Text(
        i18n.messageContentNoDecryption(),
        Modifier.padding(10.dp),
        style = MaterialTheme.typography.bodySmall, // FIXME alpha?
        fontStyle = FontStyle.Italic,
    )
}
