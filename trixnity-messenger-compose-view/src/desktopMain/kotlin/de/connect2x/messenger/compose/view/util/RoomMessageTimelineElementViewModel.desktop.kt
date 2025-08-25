package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry

import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.Location
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased
import java.awt.datatransfer.StringSelection

@Composable
actual fun RoomMessageTimelineElementViewModel<*>.toClipEntry(): ClipEntry? {
    val transferable = when (this) {
        is FileBased -> null // FIXME should deliver caption
        is Location -> StringSelection(this.coordinates) // FIXME should deliver proper location description (placename, coordinates)
        is TextBased -> StringSelection(this.body)
        is RoomMessageTimelineElementViewModel.Unknown, is RoomMessageTimelineElementViewModel.VerificationRequest -> null
    }

    return transferable?.let {
        ClipEntry(it)
    }
}

