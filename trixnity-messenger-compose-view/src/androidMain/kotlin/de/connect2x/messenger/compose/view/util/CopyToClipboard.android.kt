package de.connect2x.messenger.compose.view.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.util.CopyableContent
import de.connect2x.trixnity.messenger.util.File
import de.connect2x.trixnity.messenger.util.FormattedText
import de.connect2x.trixnity.messenger.util.Location
import de.connect2x.trixnity.messenger.util.Text
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.Base64

actual fun platformCopyToClipboardModule(): Module = module {
    single<CopyToClipboard> {
        object : CopyToClipboard {
            @Composable
            override fun invoke(): suspend (CopyableContent, I18nView) -> Unit {
                val context = LocalContext.current
                val contentResolver = context.contentResolver
                val clipboardManager = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

                return { content, i18n ->
                    val clipData = when (content) {
                        is File ->
                            ClipData(
                                content.fileName,
                                arrayOf(content.fileType.toString()),
                                ClipData.Item(
                                    Base64.getEncoder().encodeToString(content.file)
                                )
                            )

                        is Location ->
                            ClipData.newUri(
                                contentResolver,
                                content.description,
                                Uri.parse(content.geoUri)
                            )

                        is Text ->
                            ClipData.newPlainText(
                                content.text,
                                content.text
                            )

                        is FormattedText ->
                            ClipData.newHtmlText(
                                content.unformattedText,
                                content.unformattedText,
                                content.text
                            )

                        else -> ClipData.newPlainText(content.fallbackText, content.fallbackText)
                    }

                    clipboardManager.setPrimaryClip(clipData)
                    Toast.makeText(context, i18n.commonCopied(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
