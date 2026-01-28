package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.configureTestLogging
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

class TextFieldViewModelTest {
    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `should limit the text field to the max length`() {
        val cut = TextFieldViewModelImpl(maxLength = 3)
        cut.update(text = "1234", selection = 4..4)
        cut.value.text shouldBe "123"
        cut.value.selection shouldBe 3..3

        cut.update(text = "12", selection = 0..1)
        cut.value.text shouldBe "12"
        cut.value.selection shouldBe 0..1

        cut.update(text = "123456", selection = 5..6)
        cut.value.text shouldBe "123"
        cut.value.selection shouldBe 3..3
    }
}
