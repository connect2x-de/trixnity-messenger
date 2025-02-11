package de.connect2x.trixnity.messenger.viewmodel.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class InitialsComputationTest : ShouldSpec() {
    val initials = Initials

    init {
        should("resolve test initials with 1 character correctly") {
            initials.compute("test") shouldBe "T"
        }
        should("resolve test initials with 2 characters correctly") {
            initials.compute("test initials") shouldBe "TI"
        }
        should("resolve test initials with 3 characters correctly") {
            initials.compute("one two three") shouldBe "OT"
        }
        should("resolve initials with 4 byte characters") {
            initials.compute("Test 🦈") shouldBe "T🦈"
        }
        should("resolve sequence of 4 byte characters to one initial") {
            initials.compute("🦈🦈🦈🦈") shouldBe "🦈"
        }
        should("resolve initials from weirdly spaced source") {
            initials.compute("weird  \t \n spaces") shouldBe "WS"
        }
        should("resolve blank initials on empty text") {
            initials.compute("") shouldBe ""
        }
        should("resolve initials with numbers") {
            initials.compute("test 123") shouldBe "T1"
        }
        should("resolve initials with only numbers") {
            initials.compute("123 456") shouldBe "14"
        }
        should("resolve initials with arabic letters") {
            initials.compute("أمير") shouldBe "أ"
        }
        should("resolve initials with emoji containing regional indicators") {
            initials.compute("🇹🇦 Prosperity to Tristan da Cunha") shouldBe "🇹🇦P"
        }
        should("resolve initials with emoji family and skin tone modifier") {
            initials.compute("👨‍👩‍👧‍👦 👧🏿") shouldBe "👨‍👩‍👧‍👦👧🏿"
        }
        should("resolve initials with diacritics") {
            initials.compute("T̃est Înitials") shouldBe "T̃Î"
        }
    }
}
