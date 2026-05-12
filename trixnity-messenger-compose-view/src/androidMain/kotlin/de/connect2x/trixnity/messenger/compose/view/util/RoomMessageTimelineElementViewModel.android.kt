package de.connect2x.trixnity.messenger.compose.view.util

import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.Location
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased

@Composable
actual fun RoomMessageTimelineElementViewModel<*>.toClipEntry(): ClipEntry? {
    val i18n = DI.get<I18nView>()

    val clipData = when (this) {
        is FileBased -> null // TODO should deliver caption

        is Location ->
            ClipData.newPlainText(
                i18n.commonLocation(),
                this.coordinates // TODO should deliver proper location description (placename, coordinates)
            )

        is TextBased ->
            this.formattedBody?.let {
                ClipData.newHtmlText(
                    i18n.commonRichText(),
                    this.body,
                    this.formattedBody
                )
            } ?: ClipData.newPlainText(
                i18n.commonText(),
                this.body
            )

        is RoomMessageTimelineElementViewModel.Unknown,
        is RoomMessageTimelineElementViewModel.VerificationRequest -> null
    }

    return clipData?.let {
        ClipEntry(it)
    }
}
