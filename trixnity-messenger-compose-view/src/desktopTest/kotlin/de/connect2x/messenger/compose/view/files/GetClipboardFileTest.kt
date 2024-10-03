package de.connect2x.messenger.compose.view.files

import io.kotest.core.spec.style.ShouldSpec
import net.folivo.trixnity.utils.toByteArray
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.InputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


abstract class MockTransferable(val flavors: Array<DataFlavor>) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = flavors

    override fun isDataFlavorSupported(maybeFlavor: DataFlavor?): Boolean =
        maybeFlavor?.let { flavor -> flavors.any { it.match(flavor) } } ?: false

    abstract override fun getTransferData(flavor: DataFlavor?): Any
}

class CopyImageTransferable(val inputStream: InputStream) : MockTransferable(
    arrayOf(DataFlavor("image/png;class=java.io.InputStream", "")),
) {
    override fun getTransferData(flavor: DataFlavor?): Any = inputStream
}

class CopyFileTransferable(val s: String) : MockTransferable(
    arrayOf(DataFlavor("text/uri-list;class=java.io.Reader", ""))
) {

    override fun getTransferData(flavor: DataFlavor?): Any = s.reader()
}

class GetClipboardFileTest : ShouldSpec(), ClipboardOwner {

    private val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    private val fileSystem = FakeFileSystem()
    private lateinit var oldData: Transferable

    init {
        beforeSpec {
            oldData = clipboard.getContents(this)

        }

        afterSpec {
            clipboard.setContents(oldData, this)
        }

        context("getClipboardFile") {

            should("work with text/uri-list (files on disk)") {
                val fileName = "abc.txt"
                val fileContent = "hello world\n".toByteArray()

                fileSystem.write(fileName.toPath()) {
                    write(fileContent)
                }

                val transferable = CopyFileTransferable("file:///${fileName}")
                clipboard.setContents(transferable, this@GetClipboardFileTest)
                val fileDescriptor = getClipboardFile(fileSystem)

                assertNotNull(fileDescriptor)
                assertEquals(fileName, fileDescriptor.fileName)
                assertEquals(fileContent.size, fileDescriptor.fileSize)
                assertContentEquals(fileContent, fileDescriptor.content.toByteArray())
                // Check replay
                assertContentEquals(fileContent, fileDescriptor.content.toByteArray())
                assertEquals("text", fileDescriptor.mimeType?.contentType)
                assertEquals("plain", fileDescriptor.mimeType?.contentSubtype)
            }
            should("work with image/* (images in memory)") {
                val data = "hello world".toByteArray()
                val transferable = CopyImageTransferable(data.inputStream())
                clipboard.setContents(transferable, this@GetClipboardFileTest)
                val fileDescriptor = getClipboardFile(fileSystem)

                assertNotNull(fileDescriptor)
                assertEquals(data.size, fileDescriptor.fileSize)
                assertContentEquals(data, fileDescriptor.content.toByteArray())
                // check replay
                assertContentEquals(data, fileDescriptor.content.toByteArray())
                assertEquals("image", fileDescriptor.mimeType?.contentType)
                assertEquals("png", fileDescriptor.mimeType?.contentSubtype)
            }
        }
    }

    override fun lostOwnership(clipboard: Clipboard?, contents: Transferable?) {
        // ignore
    }
}
