package de.connect2x.trixnity.messenger.util

import kotlin.test.Test
import kotlin.test.assertEquals

class StringsTest {
    @Test
    fun `Determine grapheme cluster count in empty string`() {
        assertEquals(0, "".graphCount)
    }

    @Test
    fun `Determine grapheme cluster count in string without emojis`() {
        val string = "The quick brown fox jumps over the lazy brown dog"
        assertEquals(string.length, string.graphCount)
    }

    @Test
    fun `Determine grapheme cluster count in string with emojis`() {
        assertEquals(2, "\uD83E\uDD8A\uD83D\uDC36".graphCount)
    }

    @Test
    fun `Determine grapheme cluster count of regular emoji`() {
        assertEquals(1, "\uD83E\uDD8A".graphCount)
    }

    @Test
    fun `Determine grapheme cluster count of regional indicator emoji`() {
        assertEquals(1, "\uD83C\uDDE6".graphCount)
    }

    @Test
    fun `Determine grapheme cluster count of flag emoji`() {
        assertEquals(1, "\uD83C\uDDE9\uD83C\uDDEA".graphCount)
    }

    @Test
    fun `Determine grapheme cluster count of emoji with skin tone modifier`() {
        assertEquals(1, "\uD83D\uDC4C\uD83C\uDFFE".graphCount)
    }

    @Test
    fun `Determine grapheme cluster count of diacritic`() {
        assertEquals(1, "t̃".graphCount)
    }

    @Test
    fun `Determine grapheme cluster count of multiple diacritics`() {
        assertEquals(4, "t̃est̃".graphCount)
    }
}
