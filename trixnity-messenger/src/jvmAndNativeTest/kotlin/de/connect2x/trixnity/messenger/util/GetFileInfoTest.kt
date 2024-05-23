package de.connect2x.trixnity.messenger.util

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.ktor.utils.io.core.*
import net.folivo.trixnity.utils.toByteArray
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class GetFileInfoTest : ShouldSpec({
    lateinit var fakeFileSystem: FakeFileSystem
//    lateinit var cut: GetFileInfo

    beforeTest {
        fakeFileSystem = FakeFileSystem()
//        cut = GetFileInfoImpl(fakeFileSystem)
    }

//    should("create from text") {
//        val filePath = "/directory/text.txt".toPath()
//        fakeFileSystem.createDirectories("/directory".toPath())
//        fakeFileSystem.write(filePath) {
//            writeUtf8("test")
//        }
//        assertSoftly(cut(filePath).shouldNotBeNull()) {
//            fileName shouldBe "text.txt"
//            fileSize shouldBe 4
//            mimeType shouldBe ContentType.Text.Plain
//            content.toByteArray() shouldBe "test".toByteArray()
//        }
//    }
//    should("create from image") {
//        val filePath = "/directory/image.jpg".toPath()
//        fakeFileSystem.createDirectories("/directory".toPath())
//        fakeFileSystem.write(filePath) {
//            writeUtf8("image")
//        }
//        assertSoftly(cut(filePath).shouldNotBeNull()) {
//            fileName shouldBe "image.jpg"
//            fileSize shouldBe 5
//            mimeType shouldBe ContentType.Image.JPEG
//            content.toByteArray() shouldBe "image".toByteArray()
//        }
//    }
//    should("create from video") {
//        val filePath = "/directory/video.mp4".toPath()
//        fakeFileSystem.createDirectories("/directory".toPath())
//        fakeFileSystem.write(filePath) {
//            writeUtf8("video")
//        }
//        assertSoftly(cut(filePath).shouldNotBeNull()) {
//            fileName shouldBe "video.mp4"
//            fileSize shouldBe 5
//            mimeType shouldBe ContentType.Video.MP4
//            content.toByteArray() shouldBe "video".toByteArray()
//        }
//    }
})
