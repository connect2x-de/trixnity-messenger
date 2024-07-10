package de.connect2x.trixnity.messenger.viewmodel.util

import io.kotest.core.spec.style.ShouldSpec
import kotlin.test.assertEquals

class InitialsComputationTest : ShouldSpec() {
    val initials = Initials

    init {
        should("resolve test initials with 1 character correctly") {
            val initials = initials.compute("test")
            assertEquals("T", initials)
        }

        should("resolve test initials with 2 characters correctly") {
            val initials = initials.compute("test initials")
            assertEquals("TI", initials)
        }

        should("resolve test initials with 3 characters correctly") {
            val initials = initials.compute("one two three")
            assertEquals("OT", initials)
        }

        should("resolve initials with 4 byte characters") {
            val initials = initials.compute("Test 🦈")
            assertEquals("T🦈", initials)
        }

        should("resolve sequence of 4 byte characters to one initial") {
            val initials = initials.compute("🦈🦈🦈🦈")
            assertEquals("🦈", initials)
        }

        should("resolve initials from weirdly spaced source") {
            val initials = initials.compute("weird  \t \n spaces")
            assertEquals("WS", initials)
        }

        should("resolve blank initials on empty text") {
            val initials = initials.compute("")
            assertEquals("", initials)
        }

        should("resolve initials with numbers") {
            val initials = initials.compute("test 123")
            assertEquals("T1", initials)
        }

        should("resolve initials with only numbers") {
            val initials = initials.compute("123 456")
            assertEquals("14", initials)
        }

        should("resolve initials with arabic letters") {
            val initials = initials.compute("أمير")
            assertEquals("أ", initials)
        }

        // TODO: Check if we need to support emoji modifiers.
//        should("resolve initials with emoji modifiers") {
//            val initials = initials.compute("🇹🇦Prosperity to Tristan da Cunha")
//            assertEquals("🇹🇦p", initials)
//        }

        // TODO: Check if we need to support emoji modifiers.
//        should("resolve initials with emoji family and skin color") {
//            val initials = initials.compute("👨‍👩‍👧‍👦 👧🏿")
//            assertEquals("👨‍👩‍👧‍👦👧🏿", initials)
//        }
    }
}
