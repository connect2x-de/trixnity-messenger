package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.ClipboardItem
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.Location
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.Unknown
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.VerificationRequest
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun ClipEntry.Companion.withFormattedText(htmlText: String, plainText: String): ClipEntry {
    if (window.asDynamic().isSecureContext == false) {
        println("ClipboardItem is not available in insecure contexts.")
        return ClipEntry(emptyArray())
    }

    return ClipEntry(createClipboardItemWithFormattedText(htmlText, plainText))
}

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalComposeUiApi::class)
private fun createClipboardItemWithFormattedText(htmlText: String, plainText: String): Array<ClipboardItem> =
    js("[new ClipboardItem({'text/plain': new Blob([plainText], { type: 'text/plain' }), 'text/html': new Blob([htmlText], { type: 'text/html' }),})]")

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun RoomMessageTimelineElementViewModel<*>.toClipEntry(): ClipEntry? =
    when (this) {
        is TextBased<*> ->
            this.formattedBody?.let {
                ClipEntry.withFormattedText(it, this.body)
            } ?: ClipEntry.withPlainText(this.body)

        // TODO should deliver caption
        is FileBased<*> -> ClipEntry(emptyArray())

        // TODO should deliver proper location description (placename, coordinates)
        is Location -> ClipEntry.withPlainText(this.coordinates)

        is VerificationRequest,
        is Unknown -> ClipEntry(emptyArray())
    }.takeIf { it.clipboardItems.isNotEmpty() }
