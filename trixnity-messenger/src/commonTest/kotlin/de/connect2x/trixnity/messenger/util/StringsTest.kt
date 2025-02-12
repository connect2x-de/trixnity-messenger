package de.connect2x.trixnity.messenger.util

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StringsTest {
    @Test
    fun emptyGraphCount() {
        "".graphCount shouldBe 0
    }

    @Test
    fun asciiGraphCount() {
        val string = "The quick brown fox jumps over the lazy brown dog"
        string.graphCount shouldBe string.length
    }

    @Test
    fun multipleEmojiGraphCount() {
        "\uD83E\uDD8A\uD83D\uDC36".graphCount shouldBe 2
    }

    @Test
    fun singleEmojiGraphCount() {
        "\uD83E\uDD8A".graphCount shouldBe 1
    }

    @Test
    fun regionalIndicatorGraphCount() {
        "\uD83C\uDDE6".graphCount shouldBe 1
    }

    @Test
    fun flagGraphCount() {
        "\uD83C\uDDE9\uD83C\uDDEA".graphCount shouldBe 1
    }

    @Test
    fun skinToneModifierGraphCount() {
        "\uD83D\uDC4C\uD83C\uDFFE".graphCount shouldBe 1
    }

    @Test
    fun singleDiacriticGraphCount() {
        "t̃".graphCount shouldBe 1
    }

    @Test
    fun multipleDiacriticsGraphCount() {
        "t̃est̃".graphCount shouldBe 4
    }

    @Test
    fun forEachGraphInAsciiString() {
        val string = "Hello"
        string.forEachGraph { graph, index ->
            graph shouldBe string[index].toString()
            true
        }
    }

    @Test
    fun forEachGraphInEmojiString() {
        val string = "\uD83E\uDD8A\uD83D\uDC3A\uD83D\uDC10"
        string.forEachGraph { graph, index ->
            when (index) {
                0 -> graph shouldBe "\uD83E\uDD8A"
                1 -> graph shouldBe "\uD83D\uDC3A"
                2 -> graph shouldBe "\uD83D\uDC10"
            }
            true
        }
    }

    @Test
    fun forEachGraphInMixedString() {
        val string = "\uD83E\uDD8A&\uD83D\uDC3A&\uD83D\uDC10"
        string.forEachGraph { graph, index ->
            when (index) {
                0 -> graph shouldBe "\uD83E\uDD8A"
                1 -> graph shouldBe "&"
                2 -> graph shouldBe "\uD83D\uDC3A"
                3 -> graph shouldBe "&"
                4 -> graph shouldBe "\uD83D\uDC10"
            }
            true
        }
    }
}
