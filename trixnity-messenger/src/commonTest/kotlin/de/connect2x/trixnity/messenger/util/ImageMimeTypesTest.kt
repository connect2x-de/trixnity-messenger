package de.connect2x.trixnity.messenger.util

import io.kotest.matchers.shouldBe
import io.ktor.http.ContentType
import kotlin.test.Test

class ImageMimeTypesTest {
    private val cut = ImageMimeTypes

    @Test
    fun `prefer jpg for JPEG`() {
        cut.preferredFileExtension(ContentType.Image.JPEG) shouldBe "jpg"
    }

    @Test
    fun `ignore parameters when selecting extensions`() {
        cut.preferredFileExtension(ContentType.parse("IMAGE/JPEG; charset=binary")) shouldBe "jpg"
        cut.preferredFileExtension(ContentType.parse("image/png; charset=binary")) shouldBe "png"
    }

    @Test
    fun `prefer bmp for BMP`() {
        cut.preferredFileExtension(ContentType.Image.BMP) shouldBe "bmp"
    }

    @Test
    fun `fall back to Ktor for other image formats`() {
        cut.preferredFileExtension(ContentType.parse("image/x-icon")) shouldBe "ico"
    }

    @Test
    fun `unknown types have no extensions`() {
        cut.preferredFileExtension(ContentType.parse("image/unknown-type")) shouldBe null
        cut.preferredFileExtension(ContentType.Image.Any) shouldBe null
    }
}
