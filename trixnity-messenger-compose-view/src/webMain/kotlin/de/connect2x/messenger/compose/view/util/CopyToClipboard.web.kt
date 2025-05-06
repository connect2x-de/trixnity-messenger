package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.util.CopyableContent
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import js.objects.recordOf
import org.khronos.webgl.Uint8Array
import org.koin.core.module.Module
import org.koin.dsl.module
import web.blob.Blob
import web.clipboard.ClipboardItem
import web.navigator.navigator

private val log = KotlinLogging.logger { }

actual fun platformCopyToClipboardModule(): Module = module {
    single<CopyToClipboard> {
        object : CopyToClipboard {
            @Composable
            override fun create(): suspend (CopyableContent, I18nView) -> Unit {
                return { content, _ ->
                    val items = when (content) {
                        is CopyableContent.File -> {
                            val uint8Array = Uint8Array(content.file.toTypedArray())
                            val blob = Blob(
                                arrayOf(uint8Array)
                            )

                            // TODO: Investigate further why pretty much only image/png works
                            recordOf(
                                content.fileType.toString() to blob,
                                ContentType.Text.Plain.toString() to content.fileName
                            )
                        }

                        is CopyableContent.FormattedText -> recordOf(
                            ContentType.Text.Html.toString() to content.text,
                            ContentType.Text.Plain.toString() to content.unformattedText
                        )

                        is CopyableContent.Location -> recordOf(ContentType.Text.Plain.toString() to content.coordinates)
                        is CopyableContent.Text -> recordOf(ContentType.Text.Plain.toString() to content.text)
                    }

                    navigator.clipboard.write(
                        arrayOf(
                            ClipboardItem(items)
                        )
                    )
                }
            }
        }
    }
}
