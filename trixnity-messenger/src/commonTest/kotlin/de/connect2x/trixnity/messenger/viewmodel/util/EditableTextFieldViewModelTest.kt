package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModelImpl
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test


class EditableTextFieldViewModelTest {

    private val serverValue: MutableStateFlow<String> = MutableStateFlow("old value")
    private var onApplyValue: suspend (String) -> Result<*> = { newValue ->
        numCallsOnApplyValue.value += 1
        isHoldingUpOnApplyValue.first { it.not() }
        serverValue.value = newValue
        Result.success(Unit)
    }
    private val numCallsOnApplyValue: MutableStateFlow<Int> = MutableStateFlow(0)
    private val isHoldingUpOnApplyValue: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @Test
    fun `initialize the view model in the correct state`() = runTest {
        val viewModel = editableTextFieldModel()
        viewModel.textValue shouldBe ""
        viewModel.isEdit.value shouldBe false
        viewModel.isLoading.value shouldBe true

        delay(100)
        viewModel.textValue shouldBe "old value"
        viewModel.isLoading.value shouldBe false
    }

    @Test
    fun `reflect external changes in the local value`() = runTest {
        val viewModel = editableTextFieldModel()
        delay(100)

        viewModel.isEdit.value shouldBe false
        viewModel.textValue shouldBe "old value"

        serverValue.value = "new value"
        delay(100)

        viewModel.isEdit.value shouldBe false
        viewModel.textValue shouldBe "new value"
    }

    @Test
    fun `start and apply edit to affect the remote value`() = runTest {
        serverValue.value = "current value"
        val viewModel = editableTextFieldModel()
        delay(100)

        viewModel.startEdit()
        viewModel.isEdit.value shouldBe true

        viewModel.update("edited value")
        viewModel.textValue shouldBe "edited value"
        serverValue.value shouldBe "current value"
        numCallsOnApplyValue.value shouldBe 0

        viewModel.approveEdit()
        delay(100)

        viewModel.isEdit.value shouldBe false
        viewModel.isLoading.value shouldBe false
        serverValue.value shouldBe "edited value"
        numCallsOnApplyValue.value shouldBe 1
    }

    @Test
    fun `start and cancel edit without it affecting the remote value`() = runTest {
        serverValue.value = "current value"
        val viewModel = editableTextFieldModel()
        delay(100)
        viewModel.startEdit()
        viewModel.isEdit.value shouldBe true

        viewModel.update("edited value")
        viewModel.textValue shouldBe "edited value"
        serverValue.value shouldBe "current value"
        numCallsOnApplyValue.value shouldBe 0

        viewModel.cancelEdit()
        viewModel.isLoading.value shouldBe false
        viewModel.isEdit.value shouldBe false
        serverValue.value shouldBe "current value"
        numCallsOnApplyValue.value shouldBe 0
    }

    @Test
    fun `restore edit state on failure when applying value`() = runTest {
        onApplyValue = {
            numCallsOnApplyValue.value += 1
            Result.failure<Exception>(Exception("test error"))
        }
        val viewModel = editableTextFieldModel()
        delay(100)
        viewModel.startEdit()
        viewModel.update("new value")
        numCallsOnApplyValue.value shouldBe 0
        viewModel.error.value shouldBe null

        viewModel.approveEdit()
        delay(100)
        viewModel.isEdit.value shouldBe true
        viewModel.textValue shouldBe "new value"
        viewModel.error.value shouldBe "test error"
        viewModel.isLoading.value shouldBe false
        serverValue.value shouldBe "old value"
        numCallsOnApplyValue.value shouldBe 1
    }

    @Test
    fun `be loading while value is applied`() = runTest {
        isHoldingUpOnApplyValue.value = true
        val viewModel = editableTextFieldModel()
        delay(100)
        viewModel.isLoading.value shouldBe false

        viewModel.startEdit()
        viewModel.update("new value")
        viewModel.approveEdit()
        viewModel.isLoading.value shouldBe true

        isHoldingUpOnApplyValue.value = false
        delay(100)

        viewModel.isLoading.value shouldBe false
    }

    @Test
    fun `not apply changes while loading`() = runTest {
        isHoldingUpOnApplyValue.value = true
        val viewModel = editableTextFieldModel()
        delay(100)

        viewModel.startEdit()
        viewModel.update("first edit")
        viewModel.approveEdit()
        delay(100)
        numCallsOnApplyValue.value shouldBe 1

        viewModel.update("second edit")
        viewModel.approveEdit()
        numCallsOnApplyValue.value shouldBe 1
    }

    @Test
    fun `not apply changes if value doesn't differ`() = runTest {
        serverValue.value = "same value"
        val viewModel = editableTextFieldModel()
        delay(100)
        viewModel.startEdit()
        viewModel.update("same value")
        viewModel.approveEdit()
        numCallsOnApplyValue.value shouldBe 0
        serverValue.value shouldBe "same value"
    }

    private fun TestScope.editableTextFieldModel() = ApprovableTextFieldViewModelImpl(
        coroutineScope = backgroundScope,
        onApplyChange = onApplyValue,
        serverValue = serverValue,
        maxLength = 1_000,
    )
}
