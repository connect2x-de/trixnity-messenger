package de.connect2x.messenger.compose.view.util

import android.content.ClipData
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.Location
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased

@Composable
actual fun RoomMessageTimelineElementViewModel<*>.toClipEntry(): ClipEntry? {
    val context = LocalContext.current

    val clipData = when (this) {
        is FileBased ->
            this.formattedCaption?.let {
                ClipData.newHtmlText(
                    this.caption,
                    this.caption,
                    it
                )
            } ?: this.caption?.let {
                ClipData.newPlainText(it, it)
            }

        is Location ->
            ClipData.newPlainText(this.coordinates, this.coordinates)

        is TextBased ->
            this.formattedBody?.let {
                ClipData.newHtmlText(
                    this.body,
                    this.body,
                    this.formattedBody
                )
            } ?: ClipData.newPlainText(
                this.body,
                this.body
            )

        is RoomMessageTimelineElementViewModel.Unknown,
        is RoomMessageTimelineElementViewModel.VerificationRequest -> null
    }

    return clipData?.let {
        ClipEntry(it)
    }
}
