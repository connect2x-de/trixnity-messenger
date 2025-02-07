package de.connect2x.trixnity.messenger.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class StringsTest : ShouldSpec() {
    init {
        should("determine grapheme cluster count in empty string") {
            "".graphCount shouldBe 0
        }
        should("determine grapheme cluster count in string without emojis") {
            val string = "The quick brown fox jumps over the lazy brown dog"
            string.graphCount shouldBe string.length
        }
        should("determine grapheme cluster count in string with emojis") {
            "\uD83E\uDD8A\uD83D\uDC36".graphCount shouldBe 2
        }
        should("determine grapheme cluster count of regular emoji") {
            "\uD83E\uDD8A".graphCount shouldBe 1
        }
        should("determine grapheme cluster count of regional indicator emoji") {
            "\uD83C\uDDE6".graphCount shouldBe 1
        }
        should("determine grapheme cluster count of flag emoji") {
            "\uD83C\uDDE9\uD83C\uDDEA".graphCount shouldBe 1
        }
        should("determine grapheme cluster count of emoji with skin tone modifier") {
            "\uD83D\uDC4C\uD83C\uDFFE".graphCount shouldBe 1
        }
        should("determine grapheme cluster count of diacritic") {
            "t̃".graphCount shouldBe 1
        }
        should("determine grapheme cluster count of multiple diacritics") {
            "t̃est̃".graphCount shouldBe 4
        }
    }
}
