package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.FileDescriptorDesktop
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.ContentType
import net.folivo.trixnity.utils.toByteArray
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class FileDescriptorTest : ShouldSpec({
    lateinit var cut: FileDescriptor
    lateinit var fakeFileSystem: FakeFileSystem


    beforeTest {
        fakeFileSystem = FakeFileSystem()
    }

    should("create from text") {
        val filePath = "/directory/text.txt".toPath()
        fakeFileSystem.createDirectories("/directory".toPath())
        fakeFileSystem.write(filePath) {
            writeUtf8("test")
        }
        cut = FileDescriptorDesktop(filePath, fakeFileSystem)
        assertSoftly(cut.fileSize.shouldNotBeNull()) {
            cut.fileName shouldBe "text.txt"
            cut.fileSize shouldBe 4
            cut.mimeType shouldBe ContentType.Text.Plain
            cut.content.toByteArray() shouldBe "test".toByteArray()
        }
    }

    should("create from image") {
        val filePath = "/directory/image.jpg".toPath()
        fakeFileSystem.createDirectories("/directory".toPath())
        fakeFileSystem.write(filePath) {
            writeUtf8("image")
        }
        cut = FileDescriptorDesktop(filePath, fakeFileSystem)
        assertSoftly(cut.fileSize.shouldNotBeNull()) {
            cut.fileName shouldBe "image.jpg"
            cut.fileSize shouldBe 5
            cut.mimeType shouldBe ContentType.Image.JPEG
            cut.content.toByteArray() shouldBe "image".toByteArray()
        }
    }

    should("create from video") {
        val filePath = "/directory/video.mp4".toPath()
        fakeFileSystem.createDirectories("/directory".toPath())
        fakeFileSystem.write(filePath) {
            writeUtf8("video")
        }
        cut = FileDescriptorDesktop(filePath, fakeFileSystem)
        assertSoftly(cut.fileSize.shouldNotBeNull()) {
            cut.fileName shouldBe "video.mp4"
            cut.fileSize shouldBe 5
            cut.mimeType shouldBe ContentType.Video.MP4
            cut.content.toByteArray() shouldBe "video".toByteArray()
        }
    }
})
