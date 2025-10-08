package de.connect2x.trixnity.messenger.compose.view

import de.connect2x.trixnity.messenger.util.DragAndDropHandlerBase
import de.connect2x.trixnity.messenger.util.PathFileDescriptor
import io.github.oshai.kotlinlogging.KotlinLogging
import okio.FileSystem
import okio.Path.Companion.toPath
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.io.File

private val log = KotlinLogging.logger { }

class DragAndDrop(
    private val dragAndDropHandler: DragAndDropHandlerBase,
    private val fileSystem: FileSystem
) : DropTargetListener {
    override fun dragEnter(dtde: DropTargetDragEvent?) {
        dtde?.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE)
        // for some reason, in MacOS we cannot determine the file name (like in drop()) as the transferData is null
        dtde?.transferable?.getTransferData(DataFlavor.javaFileListFlavor)?.let { transferData ->
            val files = transferData as List<*>
            dragAndDropHandler.drag(files.mapNotNull {
                if (it is File) PathFileDescriptor(
                    path = it.absolutePath.toPath(),
                    fileSystem = fileSystem
                ) else null
            })
        } ?: if (dtde?.transferable?.isDataFlavorSupported(DataFlavor.javaFileListFlavor) == true) {
            dragAndDropHandler.drag(listOf())
        } else {
            log.warn { "transfer data in DnD operation was null" }
        }
    }

    override fun dragExit(dte: DropTargetEvent?) {
        dragAndDropHandler.dragExit()
    }

    override fun drop(dtde: DropTargetDropEvent?) {
        dtde?.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE)
        dtde?.transferable?.getTransferData(DataFlavor.javaFileListFlavor)?.let { transferData ->
            val files = transferData as List<*>
            dragAndDropHandler.drop(files.mapNotNull {
                if (it is File) PathFileDescriptor(
                    it.absolutePath.toPath(),
                    fileSystem
                ) else null
            })
            // after a successful drop we need to indicate to the UI that the D'n'D operation has stopped
            dragAndDropHandler.dragExit()
        } ?: log.warn { "transfer data in DnD operation was null" }
    }

    override fun dropActionChanged(dtde: DropTargetDragEvent?) {
        // empty
    }

    override fun dragOver(dtde: DropTargetDragEvent?) {
        // empty
    }

}
