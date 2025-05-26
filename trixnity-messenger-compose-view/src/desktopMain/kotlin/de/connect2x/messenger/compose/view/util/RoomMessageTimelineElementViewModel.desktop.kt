package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry

import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.Location
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.ByteArrayInputStream
import java.io.IOException

@Composable
actual fun RoomMessageTimelineElementViewModel<*>.toClipEntry(): ClipEntry? {
    val clipboardManager = Toolkit.getDefaultToolkit().systemClipboard

    val transferable = when (val content = this) {
        is FileBased -> object : Transferable {
            private val dataFlavor = DataFlavor(content.mimeType)
            override fun getTransferDataFlavors(): Array<DataFlavor> {
                return arrayOf(dataFlavor)
            }

            override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
                return flavor.equals(dataFlavor)
            }

            @Throws(UnsupportedFlavorException::class, IOException::class)
            override fun getTransferData(flavor: DataFlavor): Any {
                if (flavor.equals(dataFlavor)) {
                    return ByteArrayInputStream(content.loadMediaResult.value)
                } else {
                    throw UnsupportedFlavorException(flavor)
                }
            }
        }

        is Location -> StringSelection(content.name)
        is TextBased -> StringSelection(content.body)
        is RoomMessageTimelineElementViewModel.Unknown, is RoomMessageTimelineElementViewModel.VerificationRequest -> null
    }

    return transferable?.let {
        ClipEntry(it)
    }
}

