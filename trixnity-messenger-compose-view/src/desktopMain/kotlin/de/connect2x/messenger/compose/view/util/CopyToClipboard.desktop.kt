package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.util.CopyableContent
import de.connect2x.trixnity.messenger.util.File
import de.connect2x.trixnity.messenger.util.FormattedText
import de.connect2x.trixnity.messenger.util.Location
import de.connect2x.trixnity.messenger.util.Text
import org.koin.core.module.Module
import org.koin.dsl.module
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.ByteArrayInputStream
import java.io.IOException

actual fun platformCopyToClipboardModule(): Module = module {
    single<CopyToClipboard> {
        object : CopyToClipboard {
            @Composable
            override fun invoke(): suspend (CopyableContent, I18nView) -> Unit {
                val clipboardManager = Toolkit.getDefaultToolkit().systemClipboard

                return { content, _ ->
                    val transferable: Transferable = when (content) {
                        is File -> object : Transferable {
                            private val dataFlavor = DataFlavor(content.fileType.toString())
                            override fun getTransferDataFlavors(): Array<DataFlavor> {
                                return arrayOf(dataFlavor)
                            }

                            override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
                                return flavor.equals(dataFlavor)
                            }

                            @Throws(UnsupportedFlavorException::class, IOException::class)
                            override fun getTransferData(flavor: DataFlavor): Any {
                                if (flavor.equals(dataFlavor)) {
                                    return ByteArrayInputStream(content.file)
                                } else {
                                    throw UnsupportedFlavorException(flavor)
                                }
                            }
                        }

                        is Location -> StringSelection(content.fallbackText)
                        is Text -> StringSelection(content.text)
                        is FormattedText -> StringSelection(content.unformattedText)
                        else -> StringSelection(content.fallbackText)
                    }

                    clipboardManager.setContents(transferable, null)
                }
            }
        }
    }
}
