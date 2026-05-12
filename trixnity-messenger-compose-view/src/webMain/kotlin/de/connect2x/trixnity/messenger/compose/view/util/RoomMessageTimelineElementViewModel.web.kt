@file:OptIn(ExperimentalWasmJsInterop::class)

package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.Location
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.Unknown
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.VerificationRequest
import web.clipboard.ClipboardItem
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsArray
import kotlin.js.js
import kotlin.js.toJsArray

internal expect fun newClipEntry(entries: JsArray<ClipboardItem>): ClipEntry
internal expect fun withPlainText(text: String): ClipEntry

internal expect fun isEmpty(clipEntry: ClipEntry): Boolean

private fun withFormattedText(htmlText: String, plainText: String) : ClipEntry {
    if (!isSecureContext()) return newClipEntry(emptyArray<ClipboardItem>().toJsArray())

    return newClipEntry(createClipboardItemWithFormattedText(htmlText, plainText))
}

private fun isSecureContext(): Boolean = js("""window.isSecureContext === true""")

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalComposeUiApi::class)
private fun createClipboardItemWithFormattedText(htmlText: String, plainText: String): JsArray<ClipboardItem> =
    js("[new ClipboardItem({'text/plain': new Blob([plainText], { type: 'text/plain' }), 'text/html': new Blob([htmlText], { type: 'text/html' }),})]")

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun RoomMessageTimelineElementViewModel<*>.toClipEntry(): ClipEntry? =
    when (this) {
        is TextBased<*> ->
            this.formattedBody?.let {
                withFormattedText(it, this.body)
            } ?: withPlainText(this.body)

        // TODO should deliver caption
        is FileBased<*> -> newClipEntry(emptyArray<ClipboardItem>().toJsArray())

        // TODO should deliver proper location description (placename, coordinates)
        is Location -> withPlainText(this.coordinates)

        is VerificationRequest,
        is Unknown -> newClipEntry(emptyArray<ClipboardItem>().toJsArray())
    }.takeIf { !isEmpty(it) }
