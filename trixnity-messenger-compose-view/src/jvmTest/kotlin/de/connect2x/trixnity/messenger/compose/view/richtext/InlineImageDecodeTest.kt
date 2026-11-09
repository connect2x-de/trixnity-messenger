package de.connect2x.trixnity.messenger.compose.view.richtext

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntSize
import de.connect2x.trixnity.messenger.compose.view.files.decodeToImageBitmapOrNull
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InlineImageDecodeTest {
    @Test
    fun oversizedEmoticonPngsAreDownsampledOrUseTheTextFallback() {
        val bitmap = redImage(256, 256, "png").decodeToImageBitmapOrNull(IntSize(128, 128))
        if (bitmap != null) {
            assertTrue(bitmap.width < 256, "Decoded emoticon width must remain bounded")
            assertTrue(bitmap.height < 256, "Decoded emoticon height must remain bounded")
        }
    }

    @Test
    fun correctlySizedEmoticonPngsRender() {
        val bitmap = assertNotNull(redImage(128, 128, "png").decodeToImageBitmapOrNull(IntSize(128, 128)))
        assertEquals(128, bitmap.width)
        assertEquals(128, bitmap.height)
    }

    @Test
    fun oversizedJpegsAreDownsampledBeforeDisplay() {
        val bytes = redImage(4096, 2048, "jpeg")
        val bitmap = assertNotNull(bytes.decodeToImageBitmapOrNull(IntSize(inlineImageSize, inlineImageSize)))
        assertTrue(bitmap.width < inlineImageSize * 2)
        assertTrue(bitmap.height < inlineImageSize * 2)
        assertEquals(2f, bitmap.width.toFloat() / bitmap.height)
        val pixel = bitmap.toPixelMap()[bitmap.width / 2, bitmap.height / 2]
        assertTrue(pixel.red > 0.9f && pixel.green < 0.1f && pixel.blue < 0.1f, "$pixel")
    }

    @Test
    fun oversizedPngsAreDownsampledOrUseTheTextFallback() {
        val bitmap = redImage(4096, 2048, "png").decodeToImageBitmapOrNull(IntSize(inlineImageSize, inlineImageSize))
        if (bitmap != null) {
            assertTrue(bitmap.width < inlineImageSize * 2, "Decoded PNG width must remain bounded")
            assertTrue(bitmap.height < inlineImageSize * 2, "Decoded PNG height must remain bounded")
        }
    }

    @Test
    fun ordinaryPngsStillRender() {
        val bitmap =
            assertNotNull(redImage(64, 32, "png").decodeToImageBitmapOrNull(IntSize(inlineImageSize, inlineImageSize)))
        assertEquals(64, bitmap.width)
        assertEquals(32, bitmap.height)
        val pixel = bitmap.toPixelMap()[32, 16]
        assertTrue(pixel.red > 0.9f && pixel.green < 0.1f && pixel.blue < 0.1f, "$pixel")
    }

    @Test
    fun malformedImagesUseTheTextFallback() {
        assertNull(byteArrayOf().decodeToImageBitmapOrNull(IntSize(inlineImageSize, inlineImageSize)))
        assertNull(
            "invalid image".encodeToByteArray().decodeToImageBitmapOrNull(IntSize(inlineImageSize, inlineImageSize))
        )
    }

    private fun redImage(width: Int, height: Int, format: String): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            color = java.awt.Color.RED
            fillRect(0, 0, width, height)
            dispose()
        }
        return ByteArrayOutputStream().use {
            assertTrue(ImageIO.write(image, format, it))
            it.toByteArray()
        }
    }
}
