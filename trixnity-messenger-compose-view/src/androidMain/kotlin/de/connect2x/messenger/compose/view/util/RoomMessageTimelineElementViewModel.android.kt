package de.connect2x.messenger.compose.view.util

import android.content.ClipData
import android.content.ClipData.Item
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.Location
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased
import java.util.Base64

@Composable
actual fun RoomMessageTimelineElementViewModel<*>.toClipEntry(): ClipEntry? {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val clipData = when (this) {
        is FileBased ->
            ClipData(
                this.name,
                arrayOf(this.mimeType),
                Item(
                    Base64.getEncoder().encodeToString(this.loadMediaResult.value)
                )
            )

        is Location ->
            ClipData.newUri(
                contentResolver,
                this.name,
                Uri.parse(this.geoUri)
            )

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
