package de.connect2x.messenger.compose.view.files

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.folivo.trixnity.utils.toByteArray
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


abstract class MockTransferable(val flavors: Array<DataFlavor>) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = flavors

    override fun isDataFlavorSupported(maybeFlavor: DataFlavor?): Boolean =
        maybeFlavor?.let { flavor -> flavors.any { it.match(flavor) } } ?: false

    abstract override fun getTransferData(flavor: DataFlavor?): Any
}

class CopyImageTransferable(val data: ByteArray) : MockTransferable(
    arrayOf(DataFlavor("image/png;class=java.io.InputStream", "")),
) {
    override fun getTransferData(flavor: DataFlavor?): Any = data.inputStream()
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
                val fileDescriptor = getClipboardFile(fileSystem).getOrNull()

                fileDescriptor shouldNotBe null
                fileDescriptor?.fileName shouldBe fileName
                fileDescriptor?.fileSize shouldBe fileContent.size
                fileDescriptor?.content?.toByteArray() contentEquals fileContent
                // Check replay
                fileDescriptor?.content?.toByteArray() contentEquals fileContent

                fileDescriptor?.mimeType?.contentType shouldBe "text"
                fileDescriptor?.mimeType?.contentSubtype shouldBe "plain"
            }
            should("work with image/* (images in memory)") {
                val data = "hello world".toByteArray()
                val transferable = CopyImageTransferable(data)
                clipboard.setContents(transferable, this@GetClipboardFileTest)
                val fileDescriptor = getClipboardFile(fileSystem).getOrNull()

                assertNotNull(fileDescriptor)
                fileDescriptor shouldNotBe null
                fileDescriptor.fileSize shouldBe data.size
                fileDescriptor.content.toByteArray() contentEquals data
                // check replay
                fileDescriptor.content.toByteArray() contentEquals data

                fileDescriptor.mimeType?.contentType shouldBe "image"
                fileDescriptor.mimeType?.contentSubtype shouldBe "png"
            }
        }
    }

    override fun lostOwnership(clipboard: Clipboard?, contents: Transferable?) {
        // ignore
    }
}
