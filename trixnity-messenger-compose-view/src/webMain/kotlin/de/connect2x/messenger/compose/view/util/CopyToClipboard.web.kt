package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.util.CopyableContent
import io.ktor.http.ContentType
import js.objects.ReadonlyRecord
import org.khronos.webgl.Uint8Array
import org.koin.core.module.Module
import org.koin.dsl.module
import web.blob.Blob
import web.blob.BlobPropertyBag
import web.blob.EndingType
import web.clipboard.ClipboardItem
import web.navigator.navigator

private fun String.asBlob() = Blob(arrayOf(this))
private fun <B> readonlyRecordOf(vararg elements: Pair<String, B>): ReadonlyRecord<String, B> {
    return object : ReadonlyRecord<String, B> {
        private val entries: Map<String, B> =
            buildMap { elements.forEach { put(it.first, it.second) } }.toMap()

        override fun get(key: String): B? {
            return entries[key]
        }
    }
}

actual fun platformCopyToClipboardModule(): Module = module {
    single<CopyToClipboard> {
        object : CopyToClipboard {
            @Composable
            override fun create(): suspend (CopyableContent, I18nView) -> Unit {
                return { content, _ ->
                    val (type, blob) = when (content) {
                        is CopyableContent.File -> {
                            val uint8Array = Uint8Array(content.file.toTypedArray())
                            val blob = Blob(
                                arrayOf(uint8Array),
                                object : BlobPropertyBag {
                                    override val endings = EndingType.transparent
                                    override val type: String = content.fileType.toString()
                                }
                            )

                            content.fileType to blob
                        }

                        is CopyableContent.FormattedText -> ContentType.Text.Html to content.text.asBlob()
                        is CopyableContent.Location -> ContentType.Text.Plain to content.coordinates.asBlob()
                        is CopyableContent.Text -> ContentType.Text.Plain to content.text.asBlob()
                    }


                    navigator.clipboard.write(
                        arrayOf(
                            ClipboardItem(readonlyRecordOf(Pair(type.toString(), blob)))
                        )
                    )
                }
            }
        }
    }
}
