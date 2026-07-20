@file:OptIn(ExperimentalWasmJsInterop::class)

package de.connect2x.trixnity.messenger.export

import io.kotest.matchers.shouldBe
import js.promise.await
import js.string.JsStrings.toKotlinString
import js.typedarrays.toByteArray
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toList
import kotlin.test.Test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import web.blob.bytes
import web.fs.getDirectoryHandle
import web.fs.getFile
import web.fs.getFileHandle
import web.navigator.navigator
import web.storage.getDirectory
import zipjs.BlobReader
import zipjs.BlobWriter
import zipjs.TextWriter
import zipjs.ZipReader

class FileBasedExportRoomSinkWriterTest {
    @Test
    fun `first export contains events and media`() = runTest {
        val storage = navigator.storage.getDirectory()

        val fileName = "events.txt"
        val firstMedia = byteArrayOf(1, 2, 3, 4)
        val secondMedia = byteArrayOf(5, 6, 7, 8)
        val cut = WebZipFileBasedExportRoomSinkWriter(fileName)

        cut.start()
        cut.addContent("first ")
        cut.addContent("export")
        cut.addMedia(flowOf(firstMedia.copyOfRange(0, 2), firstMedia.copyOfRange(2, 4)), "first")
        cut.addMedia(flowOf(secondMedia.copyOfRange(0, 2), secondMedia.copyOfRange(2, 4)), "second")
        cut.finish()

        val archive = storage.getDirectoryHandle("room-export").getFileHandle("archive.tmp").getFile()
        val zipReader = ZipReader(BlobReader(archive))
        val entries = zipReader.getEntries().await().toList().associateBy { it.filename }

        entries.keys.sorted() shouldBe listOf(fileName, "media/first", "media/second")
        entries.getValue(fileName).getData(TextWriter()).await().toKotlinString() shouldBe "first export"
        entries.getValue("media/first").getData(BlobWriter()).await().bytes().toByteArray() shouldBe firstMedia
        entries.getValue("media/second").getData(BlobWriter()).await().bytes().toByteArray() shouldBe secondMedia

        zipReader.close().await()
    }
}
