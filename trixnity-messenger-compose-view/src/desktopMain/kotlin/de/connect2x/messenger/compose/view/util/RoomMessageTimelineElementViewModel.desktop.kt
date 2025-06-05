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
    val transferable = when (val content = this) {
        is FileBased -> content.caption?.let { StringSelection(it) }
        is Location -> StringSelection(content.name)
        is TextBased -> StringSelection(content.body)
        is RoomMessageTimelineElementViewModel.Unknown, is RoomMessageTimelineElementViewModel.VerificationRequest -> null
    }

    return transferable?.let {
        ClipEntry(it)
    }
}

