package de.connect2x.trixnity.messenger.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class StringsTest : ShouldSpec() {
    init {
        should("determine grapheme cluster count in empty string") {
            "".graphemeClusters shouldBe 0
        }
        should("determine grapheme cluster count in string without emojis") {
            val string = "The quick brown fox jumps over the lazy brown dog"
            string.graphemeClusters shouldBe string.length
        }
        should("determine grapheme cluster count in string with emojis") {
            "\uD83E\uDD8A\uD83D\uDC36".graphemeClusters shouldBe 2
        }
        should("determine grapheme cluster count of regular emoji") {
            "\uD83E\uDD8A".graphemeClusters shouldBe 1
        }
        should("determine grapheme cluster count of regional indicator emoji") {
            "\uD83C\uDDE6".graphemeClusters shouldBe 1
        }
        should("determine grapheme cluster count of flag emoji") {
            "\uD83C\uDDE9\uD83C\uDDEA".graphemeClusters shouldBe 1
        }
        should("determine grapheme cluster count of emoji with skin tone modifier") {
            "\uD83D\uDC4C\uD83C\uDFFE".graphemeClusters shouldBe 1
        }
        should("determine grapheme cluster count of diacritic") {
            "t̃".graphemeClusters shouldBe 1
        }
        should("determine grapheme cluster count of multiple diacritics") {
            "t̃est̃".graphemeClusters shouldBe 4
        }
    }
}
