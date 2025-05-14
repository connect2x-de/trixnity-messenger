package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.ClipboardItem
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.util.CopyableContent
import io.ktor.http.ContentType
import org.khronos.webgl.Uint8Array
import org.koin.core.module.Module
import org.koin.dsl.module
import org.w3c.files.Blob
import kotlin.js.Promise
import web.clipboard.ClipboardItem as NativeClipboardItem

private fun <T : Any> T.asBlob() = Blob(arrayOf(this))

actual fun platformCopyToClipboardModule(): Module = module {
    single<ToClipboardEntry> {
        object : ToClipboardEntry {
            @Composable
            @OptIn(ExperimentalComposeUiApi::class)
            override fun invoke(): suspend (CopyableContent, I18nView) -> ClipEntry {
                return { content, _ ->
                    val items = when (content) {
                        is CopyableContent.File -> {
                            val uint8Array = Uint8Array(content.file.toTypedArray())

                            if (NativeClipboardItem.supports(content.fileType.toString())) {
                                mapOf(
                                    content.fileType to uint8Array,
                                    ContentType.Text.Plain to content.fileName
                                )
                            } else {
                                mapOf(ContentType.Text.Plain to content.fileName)
                            }
                        }

                        is CopyableContent.FormattedText -> mapOf(
                            ContentType.Text.Html to content.text,
                            ContentType.Text.Plain.toString() to content.unformattedText
                        )

                        is CopyableContent.Location -> mapOf(ContentType.Text.Plain to content.coordinates)
                        is CopyableContent.Text -> mapOf(ContentType.Text.Plain to content.text)
                    }.mapKeys { it.toString() }.mapValues { it.asBlob() }

                    ClipEntry(
                        arrayOf(
                            object : ClipboardItem {
                                override val types: Array<String> = items.keys.toTypedArray()

                                override fun getType(type: String): Promise<Blob> {
                                    return Promise.resolve(
                                        items[type] ?:
                                        items[ContentType.Text.Plain.toString()]!!
                                    )
                                }
                            }
                        )
                    )
                }
            }
        }
    }
}
