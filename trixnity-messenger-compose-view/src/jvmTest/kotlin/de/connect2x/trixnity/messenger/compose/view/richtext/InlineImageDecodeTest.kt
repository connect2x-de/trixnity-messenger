package de.connect2x.trixnity.messenger.compose.view.richtext

import androidx.compose.ui.graphics.toPixelMap
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
    fun oversizedJpegsAreDownsampledBeforeDisplay() {
        val bytes = redImage(4096, 2048, "jpeg")
        val bitmap = assertNotNull(bytes.decodeToImageBitmapOrNull(inlineImageDecodeSize))
        assertTrue(bitmap.width < inlineImageDecodeSize.width * 2)
        assertTrue(bitmap.height < inlineImageDecodeSize.height * 2)
        assertEquals(2f, bitmap.width.toFloat() / bitmap.height)
        val pixel = bitmap.toPixelMap()[bitmap.width / 2, bitmap.height / 2]
        assertTrue(pixel.red > 0.9f && pixel.green < 0.1f && pixel.blue < 0.1f, "$pixel")
    }

    @Test
    fun oversizedPngsAreDownsampledOrUseTheTextFallback() {
        val bitmap = redImage(4096, 2048, "png").decodeToImageBitmapOrNull(inlineImageDecodeSize)
        if (bitmap != null) {
            assertTrue(bitmap.width < inlineImageDecodeSize.width * 2, "Decoded PNG width must remain bounded")
            assertTrue(bitmap.height < inlineImageDecodeSize.height * 2, "Decoded PNG height must remain bounded")
        }
    }

    @Test
    fun ordinaryPngsStillRender() {
        val bitmap = assertNotNull(redImage(64, 32, "png").decodeToImageBitmapOrNull(inlineImageDecodeSize))
        assertEquals(64, bitmap.width)
        assertEquals(32, bitmap.height)
        val pixel = bitmap.toPixelMap()[32, 16]
        assertTrue(pixel.red > 0.9f && pixel.green < 0.1f && pixel.blue < 0.1f, "$pixel")
    }

    @Test
    fun malformedImagesUseTheTextFallback() {
        assertNull(byteArrayOf().decodeToImageBitmapOrNull(inlineImageDecodeSize))
        assertNull("invalid image".encodeToByteArray().decodeToImageBitmapOrNull(inlineImageDecodeSize))
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
