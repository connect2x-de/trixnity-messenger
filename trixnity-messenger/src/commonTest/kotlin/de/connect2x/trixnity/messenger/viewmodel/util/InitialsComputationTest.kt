package de.connect2x.trixnity.messenger.viewmodel.util

import io.kotest.core.spec.style.ShouldSpec
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import kotlin.test.assertEquals

class InitialsComputationTest : ShouldSpec() {
    val mocker = Mocker()

    @Mock
    lateinit var initialsMock: Initials

    init {
        beforeTest {
            mocker.reset()
            injectMocks(mocker)
        }

        should("resolve test initials with 1 character correctly") {
            val initials = initialsMock.compute("test")
            assertEquals("T", initials)
        }

        should("resolve test initials with 2 characters correctly") {
            val initials = initialsMock.compute("test initials")
            assertEquals("TI", initials)
        }

        should("resolve test initials with 3 characters correctly") {
            val initials = initialsMock.compute("one two three")
            assertEquals("OT", initials)
        }

        should("resolve initials with 4 byte characters") {
            val initials = initialsMock.compute("Test 🦈")
            assertEquals("T🦈", initials)
        }

        should("resolve sequence of 4 byte characters to one initial") {
            val initials = initialsMock.compute("🦈🦈🦈🦈")
            assertEquals("🦈", initials)
        }

        should("resolve initials from weirdly spaced source") {
            val initials = initialsMock.compute("weird  \t \n spaces")
            assertEquals("WS", initials)
        }

        should("resolve blank initials on empty text") {
            val initials = initialsMock.compute("")
            assertEquals("", initials)
        }

        should("resolve initials with numbers") {
            val initials = initialsMock.compute("test 123")
            assertEquals("T1", initials)
        }

        should("resolve initials with only numbers") {
            val initials = initialsMock.compute("123 456")
            assertEquals("14", initials)
        }

        should("resolve initials with arabic letters") {
            val initials = initialsMock.compute("أمير")
            assertEquals("أ", initials)
        }

        // TODO: Check if we need to support emoji modifiers.
//        should("resolve initials with emoji modifiers") {
//            val initials = initialsMock.compute("🇹🇦Prosperity to Tristan da Cunha")
//            assertEquals("🇹🇦p", initials)
//        }

        // TODO: Check if we need to support emoji modifiers.
//        should("resolve initials with emoji family and skin color") {
//            val initials = initialsMock.compute("👨‍👩‍👧‍👦 👧🏿")
//            assertEquals("👨‍👩‍👧‍👦👧🏿", initials)
//        }
    }
}
