package de.connect2x.trixnity.messenger.util

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.ContentType
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.utils.toByteArray
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test

class PathFileDescriptorTest {
    var fakeFileSystem: FakeFileSystem = FakeFileSystem()

    @Test
    fun `create from text`() = runTest {
        val filePath = "/directory/text.txt".toPath()
        fakeFileSystem.createDirectories("/directory".toPath())
        fakeFileSystem.write(filePath) {
            writeUtf8("test")
        }
        val cut = PathFileDescriptor(filePath, fakeFileSystem)
        assertSoftly(cut.fileSize.shouldNotBeNull()) {
            cut.fileName shouldBe "text.txt"
            cut.fileSize shouldBe 4
            cut.mimeType shouldBe ContentType.Text.Plain
            cut.content.toByteArray() shouldBe "test".toByteArray()
        }
    }

    @Test
    fun `create from image`() = runTest {
        val filePath = "/directory/image.jpg".toPath()
        fakeFileSystem.createDirectories("/directory".toPath())
        fakeFileSystem.write(filePath) {
            writeUtf8("image")
        }
        val cut = PathFileDescriptor(filePath, fakeFileSystem)
        assertSoftly(cut.fileSize.shouldNotBeNull()) {
            cut.fileName shouldBe "image.jpg"
            cut.fileSize shouldBe 5
            cut.mimeType shouldBe ContentType.Image.JPEG
            cut.content.toByteArray() shouldBe "image".toByteArray()
        }
    }

    @Test
    fun `create from video`() = runTest {
        val filePath = "/directory/video.mp4".toPath()
        fakeFileSystem.createDirectories("/directory".toPath())
        fakeFileSystem.write(filePath) {
            writeUtf8("video")
        }
        val cut = PathFileDescriptor(filePath, fakeFileSystem)
        assertSoftly(cut.fileSize.shouldNotBeNull()) {
            cut.fileName shouldBe "video.mp4"
            cut.fileSize shouldBe 5
            cut.mimeType shouldBe ContentType.Video.MP4
            cut.content.toByteArray() shouldBe "video".toByteArray()
        }
    }
}
