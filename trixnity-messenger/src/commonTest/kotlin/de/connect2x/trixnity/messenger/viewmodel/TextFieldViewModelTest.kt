package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.configureTestLogging
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

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

    @Test
    fun `should emit an error`() = runTest {
        val errorMsg = "error message"
        val errorText = "err"
        val cut =
            TextFieldViewModelImpl(maxLength = 100) {
                when (it) {
                    errorText -> errorMsg
                    else -> null
                }
            }
        cut.value.text shouldBe ""
        cut.error.first() shouldBe null

        val newText = "aaaaaa"
        cut.update(newText)
        cut.value.text shouldBe newText
        cut.error.first() shouldBe null

        cut.update(errorText)
        cut.value.text shouldBe errorText
        cut.error.first() shouldBe errorMsg
    }
}
