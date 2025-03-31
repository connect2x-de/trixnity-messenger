package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.util.testGraphemeIterableProvider
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class InitialsComputationTest {
    val initials: Initials = InitialsImpl(testGraphemeIterableProvider())

    @Test
    fun resolveInitialsWith1Character() {
        initials.compute("test") shouldBe "T"
    }

    @Test
    fun resolveInitialsWith2Characters() {
        initials.compute("test initials") shouldBe "TI"
    }

    @Test
    fun resolveInitialsWith3Characters() {
        initials.compute("one two three") shouldBe "OT"
    }

    @Test
    fun resolveInitialsWithWordAndEmoji() {
        initials.compute("Test 🦈") shouldBe "T🦈"
    }

    @Test
    fun resolveInitialsWithEmojiAndWord() {
        initials.compute("🦈 Test") shouldBe "🦈T"
    }

    @Test
    fun resolveInitialsWithMultipleEmojis() {
        initials.compute("🦈🦈🦈🦈") shouldBe "🦈"
    }

    @Test
    fun resolveInitialsFromWeirdlySpacedSource() {
        initials.compute("weird  \t \n spaces") shouldBe "WS"
    }

    @Test
    fun resolveBlankInitials() {
        initials.compute("") shouldBe ""
    }

    @Test
    fun resolveInitialsWithNumbers() {
        initials.compute("test 123") shouldBe "T1"
    }

    @Test
    fun resolveInitialsFromNumbers() {
        initials.compute("123 456") shouldBe "14"
    }

    @Test
    fun resolveInitialsWithArabicLetters() {
        initials.compute("أمير") shouldBe "أ"
    }

    @Test
    fun resolveInitialsWithRegionalIndicators() {
        initials.compute("🇹🇦 Prosperity to Tristan da Cunha") shouldBe "🇹🇦P"
    }

    @Test
    fun resolveInitialsWithSkinToneModifier() {
        initials.compute("👨‍👩‍👧‍👦 👧🏿") shouldBe "👨‍👩‍👧‍👦👧🏿"
    }

    @Test
    fun resolveInitialsWithDiacritics() {
        initials.compute("T̃est Înitials") shouldBe "T̃Î"
    }
}
