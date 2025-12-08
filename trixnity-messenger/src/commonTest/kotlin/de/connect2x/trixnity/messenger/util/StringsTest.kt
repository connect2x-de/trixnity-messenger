package de.connect2x.trixnity.messenger.util

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StringsTest {
    private val graphemeProvider = testGraphemeIterableProvider()

    @Test
    fun emptyGraphCount() {
        graphemeProvider("").graphemeCount shouldBe 0
    }

    @Test
    fun asciiGraphCount() {
        val string = "The quick brown fox jumps over the lazy brown dog"
        graphemeProvider(string).graphemeCount shouldBe string.length
    }

    @Test
    fun multipleEmojiGraphCount() {
        graphemeProvider("\uD83E\uDD8A\uD83D\uDC36").graphemeCount shouldBe 2
    }

    @Test
    fun singleEmojiGraphCount() {
        graphemeProvider("\uD83E\uDD8A").graphemeCount shouldBe 1
    }

    @Test
    fun regionalIndicatorGraphCount() {
        graphemeProvider("\uD83C\uDDE6").graphemeCount shouldBe 1
    }

    @Test
    fun flagGraphCount() {
        graphemeProvider("\uD83C\uDDE9\uD83C\uDDEA").graphemeCount shouldBe 1
    }

    @Test
    fun skinToneModifierGraphCount() {
        graphemeProvider("\uD83D\uDC4C\uD83C\uDFFE").graphemeCount shouldBe 1
    }

    @Test
    fun singleDiacriticGraphCount() {
        graphemeProvider("t̃").graphemeCount shouldBe 1
    }

    @Test
    fun multipleDiacriticsGraphCount() {
        graphemeProvider("t̃est̃").graphemeCount shouldBe 4
    }

    @Test
    fun forEachGraphInAsciiString() {
        val string = "Hello"
        graphemeProvider(string).forEachIndexed { index, graph ->
            graph shouldBe string[index].toString()
        }
    }

    @Test
    fun forEachGraphInEmojiString() {
        val string = "\uD83E\uDD8A\uD83D\uDC3A\uD83D\uDC10"
        graphemeProvider(string).forEachIndexed { index, graph ->
            when (index) {
                0 -> graph shouldBe "\uD83E\uDD8A"
                1 -> graph shouldBe "\uD83D\uDC3A"
                2 -> graph shouldBe "\uD83D\uDC10"
            }
        }
    }

    @Test
    fun forEachGraphInMixedString() {
        val string = "\uD83E\uDD8A&\uD83D\uDC3A&\uD83D\uDC10"
        graphemeProvider(string).forEachIndexed { index, graph ->
            when (index) {
                0 -> graph shouldBe "\uD83E\uDD8A"
                1 -> graph shouldBe "&"
                2 -> graph shouldBe "\uD83D\uDC3A"
                3 -> graph shouldBe "&"
                4 -> graph shouldBe "\uD83D\uDC10"
            }
        }
    }
}
