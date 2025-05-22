package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.util.CopyableContent
import io.ktor.http.ContentType
import js.objects.recordOf
import org.khronos.webgl.Uint8Array
import org.koin.core.module.Module
import org.koin.dsl.module
import org.w3c.files.Blob
import web.clipboard.ClipboardItem
import web.navigator.navigator
import web.clipboard.ClipboardItem as NativeClipboardItem

private fun <T : Any> T.asBlob() = Blob(arrayOf(this))

actual fun platformCopyToClipboardModule(): Module = module {
    single<CopyToClipboard> {
        object : CopyToClipboard {
            @Composable
            @OptIn(ExperimentalComposeUiApi::class)
            override fun invoke(): suspend (CopyableContent, I18nView) -> Unit {
                return { content, _ ->
                    val items = when (content) {
                        is CopyableContent.File -> {
                            val uint8Array = Uint8Array(content.file.toTypedArray())

                            if (NativeClipboardItem.supports(content.fileType.toString())) {
                                recordOf(
                                    content.fileType.toString() to uint8Array.asBlob(),
                                    ContentType.Text.Plain.toString() to content.fileName.asBlob()
                                )
                            } else {
                                recordOf(ContentType.Text.Plain.toString() to content.fileName.asBlob())
                            }
                        }

                        is CopyableContent.FormattedText -> recordOf(
                            ContentType.Text.Html.toString() to content.text.asBlob(),
                            ContentType.Text.Plain.toString() to content.unformattedText.asBlob()
                        )

                        is CopyableContent.Location -> recordOf(ContentType.Text.Plain.toString() to content.coordinates.asBlob())
                        is CopyableContent.Text -> recordOf(ContentType.Text.Plain.toString() to content.text.asBlob())
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
