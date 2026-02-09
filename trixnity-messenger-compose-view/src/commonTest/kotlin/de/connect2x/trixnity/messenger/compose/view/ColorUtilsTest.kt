package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.ui.graphics.Color
import de.connect2x.trixnity.messenger.compose.view.common.contrastByLuminance
import de.connect2x.trixnity.messenger.compose.view.common.toHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ColorUtilsTest {

    private val brightColor = Color.White
    private val darkColor = Color.Black

    @Test
    fun shouldYieldBrightColor() {
        listOf(
            Color.Blue,
            Color.Black,
            Color.DarkGray,
        ).forEach { tintColor ->
            val contrastColor = tintColor.contrastByLuminance(brightColor, darkColor)
            assertEquals(
                contrastColor.value, brightColor.value
            )
        }
    }

    @Test
    fun shouldYieldDarkColor() {
        listOf(
            Color.Yellow,
            Color.White,
            Color.Cyan,
        ).forEach { tintColor ->
            val contrastColor = tintColor.contrastByLuminance(brightColor, darkColor)
            assertEquals(
                contrastColor.value, darkColor.value
            )
        }
    }

    @Test
    fun shouldAssertOnIllegalArguments() {
        val tintColor = Color.Cyan
        assertFails {
            tintColor.contrastByLuminance(darkColor, brightColor)
        }
    }

    @Test
    fun shouldCalculateCorrectHexString() {
        listOf(
            Color(0xff, 0x44, 0x00) to "#ff4400ff",
            Color(0xff, 0x00, 0x00) to "#ff0000ff",
            Color(0x00, 0x00, 0x00) to "#000000ff",
            Color(0xff, 0xff, 0xff, 0xff) to "#ffffffff",
            Color(0xff, 0x00, 0xff, 0xaa) to "#ff00ffaa",
        ).forEach { testData ->
            assertEquals(
                testData.second, testData.first.toHex(),
            )
        }
    }
}
