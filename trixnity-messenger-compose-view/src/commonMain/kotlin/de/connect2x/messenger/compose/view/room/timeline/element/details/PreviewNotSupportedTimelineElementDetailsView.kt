package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlin.reflect.KClass

class PreviewNotSupportedTimelineElementDetailsView() :
    TimelineElementDetailsView<RoomMessageTimelineElementViewModel.FileBased<*>> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.FileBased<*>> =
        RoomMessageTimelineElementViewModel.FileBased::class

    override fun supportsMimeType(mimeType: String): Boolean {
        return true
    }

    @Composable
    override fun create(
        element: RoomMessageTimelineElementViewModel.FileBased<*>,
        onSave: () -> Unit,
        onClose: () -> Unit
    ) {
        FileBasedDetailsDialog(element, onSave, onClose) {
            val i18n = DI.get<I18nView>()
            Box(Modifier.fillMaxSize()) {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    Column {
                        Icon(
                            MaterialTheme.messengerIcons.typeFile, i18n.commonFile(),
                            Modifier.size(96.dp).align(Alignment.CenterHorizontally),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            i18n.fileOverlayPreviewNotSupported(),
                            Modifier.align(Alignment.CenterHorizontally),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }

}
