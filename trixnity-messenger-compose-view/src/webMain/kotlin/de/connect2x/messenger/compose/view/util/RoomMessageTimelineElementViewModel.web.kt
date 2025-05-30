package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.ClipboardItem
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased
import io.ktor.http.ContentType
import org.khronos.webgl.Uint8Array
import org.w3c.files.Blob
import web.errors.DOMException
import web.errors.DOMException.Companion.NotFoundError
import web.clipboard.ClipboardItem as NativeClipboardItem
import kotlin.js.Promise

private fun <T : Any> T.asBlob() = Blob(arrayOf(this))

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun RoomMessageTimelineElementViewModel<*>.toClipEntry(): ClipEntry? {
    val items = when (this) {
        is RoomMessageTimelineElementViewModel.TextBased<*> ->
            this.formattedBody?.let {
                mapOf(
                    ContentType.Text.Html to it,
                    ContentType.Text.Plain to this.body
                )
            } ?: mapOf(
                ContentType.Text.Plain to this.body
            )


        is FileBased<*> ->
            if (
                this.loadMediaProgress.value?.percent == 1f &&
                this.mimeType != null &&
                NativeClipboardItem.supports(this.mimeType ?: "")
            ) {
                this.loadMediaResult.value?.let {
                    mapOf(
                        this.mimeType to Uint8Array(it.toTypedArray()),
                        ContentType.Text.Plain to this.name
                    )
                } ?: mapOf(ContentType.Text.Plain to this.name)
            } else {
                mapOf(ContentType.Text.Plain to this.name)
            }

        is RoomMessageTimelineElementViewModel.Location -> mapOf(
            ContentType.Text.Html to "<a href=\"${this.geoUri}\">${this.name}</a>",
            ContentType.Text.Plain to this.name
        )

        is RoomMessageTimelineElementViewModel.VerificationRequest,
        is RoomMessageTimelineElementViewModel.Unknown -> mapOf<Any, Any>()
    }.filter { it.key == null }.mapKeys { it.toString() }.mapValues { it.asBlob() }

    return if (items.isEmpty()) null else ClipEntry(
        arrayOf(
            object : ClipboardItem {
                override val types: Array<String> = items.keys.toTypedArray()

                override fun getType(type: String): Promise<Blob> {
                    return items[type]?.let {
                        Promise.resolve(it)
                    } ?: Promise.reject(DOMException("No representation of $type found", NotFoundError))
                }
            }
        )
    )
}
