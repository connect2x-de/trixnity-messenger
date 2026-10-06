package de.connect2x.trixnity.messenger.compose.view.richtext

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class InlineImageSizeTest {
    @Test
    fun customEmotesAreIdentifiedByAttributePresenceRegardlessOfValue() {
        for (marker in listOf("", "false", "true", "anything")) {
            val attributes = mapOf("data-mx-emoticon" to marker, "width" to "999", "height" to "999")
            assertEquals(Size(64f, 32f), inlineImageSize(attributes, IntSize(100, 50), 32f, 320f))
        }
        assertEquals(Size(100f, 50f), inlineImageSize(emptyMap(), IntSize(100, 50), 32f, 320f))
    }

    @Test
    fun ordinaryImagesHonorDimensionsAndPreserveAspectRatioWhenOneIsOmitted() {
        assertEquals(Size(80f, 40f), inlineImageSize(mapOf("height" to "40"), IntSize(100, 50), 32f, 320f))
        assertEquals(Size(80f, 40f), inlineImageSize(mapOf("width" to "80"), IntSize(100, 50), 32f, 320f))
        assertEquals(
            Size(80f, 60f),
            inlineImageSize(mapOf("width" to "80", "height" to "60"), IntSize(100, 50), 32f, 320f),
        )
    }

    @Test
    fun imagesStayWithinDisplayBounds() {
        assertEquals(Size(320f, 160f), inlineImageSize(emptyMap(), IntSize(4096, 2048), 32f, 320f))
        assertEquals(Size(80f, 40f), inlineImageSize(emptyMap(), IntSize(4096, 2048), 32f, 80f))
        assertEquals(Size(160f, 320f), inlineImageSize(emptyMap(), IntSize(2048, 4096), 32f, 320f))
    }

    @Test
    fun invalidDimensionsAreIgnored() {
        for (value in listOf("-1", "NaN", "Infinity", "100%", "1.5", "999999999999999999999")) {
            assertEquals(
                Size(100f, 50f),
                inlineImageSize(mapOf("width" to value, "height" to value), IntSize(100, 50), 32f, 320f),
            )
        }
    }
}
