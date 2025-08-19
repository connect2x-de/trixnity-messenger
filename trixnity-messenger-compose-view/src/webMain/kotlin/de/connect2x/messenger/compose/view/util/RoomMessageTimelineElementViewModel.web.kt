package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.ClipboardItem
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.*
import io.ktor.http.ContentType
import org.w3c.files.Blob
import web.errors.DOMException
import web.errors.DOMException.Companion.NotFoundError
import kotlin.js.Promise

private fun <T : Any> T.asBlob() = Blob(arrayOf(this))

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun RoomMessageTimelineElementViewModel<*>.toClipEntry(): ClipEntry? {
    val items = when (this) {
        is TextBased<*> ->
            this.formattedBody?.let {
                mapOf(
                    ContentType.Text.Html to it,
                    ContentType.Text.Plain to this.body
                )
            } ?: mapOf(
                ContentType.Text.Plain to this.body
            )


        is FileBased<*> -> mapOf<Any, Any>() // FIXME should deliver caption

        is Location -> mapOf(
            ContentType.Text.Plain to this.coordinates // FIXME should deliver proper location description (placename, coordinates)
        )

        is VerificationRequest,
        is Unknown -> mapOf<Any, Any>()
    }.mapKeys { it.toString() }.mapValues { it.asBlob() }

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
