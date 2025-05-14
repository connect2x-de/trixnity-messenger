package de.connect2x.messenger.compose.view.util

import android.content.ClipData
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalContext
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.util.CopyableContent
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.Base64

actual fun platformCopyToClipboardModule(): Module = module {
    single<ToClipboardEntry> {
        object : ToClipboardEntry {
            @Composable
            override fun invoke(): suspend (CopyableContent, I18nView) -> ClipEntry {
                val context = LocalContext.current
                val contentResolver = context.contentResolver

                return { content, i18n ->
                    val clipData = when (content) {
                        is CopyableContent.File ->
                            ClipData(
                                content.fileName,
                                arrayOf(content.fileType.toString()),
                                ClipData.Item(
                                    Base64.getEncoder().encodeToString(content.file)
                                )
                            )

                        is CopyableContent.Location ->
                            ClipData.newUri(
                                contentResolver,
                                content.description,
                                Uri.parse(content.geoUri)
                            )

                        is CopyableContent.Text ->
                            ClipData.newPlainText(
                                content.text,
                                content.text
                            )

                        is CopyableContent.FormattedText ->
                            ClipData.newHtmlText(
                                content.unformattedText,
                                content.unformattedText,
                                content.text
                            )
                    }

                    Toast.makeText(context, i18n.commonCopied(), Toast.LENGTH_SHORT).show()
                    ClipEntry(clipData)
                }
            }
        }
    }
}
