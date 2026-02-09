package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.Location
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased

@Composable
@OptIn(ExperimentalComposeUiApi::class)
actual fun RoomMessageTimelineElementViewModel<*>.toClipEntry(): ClipEntry? {
    return when (this) {
        is FileBased -> null // TODO should deliver caption
        is Location -> ClipEntry.withPlainText(this.coordinates) // TODO should deliver proper location description (placename, coordinates)
        is TextBased -> ClipEntry.withPlainText(this.body)
        is RoomMessageTimelineElementViewModel.Unknown, is RoomMessageTimelineElementViewModel.VerificationRequest -> null
    }
}
