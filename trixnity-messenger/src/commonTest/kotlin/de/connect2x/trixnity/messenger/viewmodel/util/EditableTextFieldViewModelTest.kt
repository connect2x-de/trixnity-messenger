package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModelImpl
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.advanceUntilIdle
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.CoroutineContext


class EditableTextFieldViewModelTest : ShouldSpec() {

    private lateinit var serverValue: MutableStateFlow<String>
    private lateinit var onApplyValue: suspend (String) -> Result<*>
    private lateinit var numCallsOnApplyValue: MutableStateFlow<Int>
    private lateinit var isHoldingUpOnApplyValue: MutableStateFlow<Boolean>

    init {
        coroutineTestScope = true

        beforeTest {
            serverValue = MutableStateFlow("old value")
            numCallsOnApplyValue = MutableStateFlow(0)
            isHoldingUpOnApplyValue = MutableStateFlow(false)
            onApplyValue = { newValue ->
                numCallsOnApplyValue.value += 1
                isHoldingUpOnApplyValue.first { it.not() }
                serverValue.value = newValue
                Result.success(Unit)
            }
        }

        withCoroutinesShould("initialize the view model in the correct state") {
            val viewModel = editableTextFieldModel(coroutineContext)
            viewModel.textValue shouldBe ""
            viewModel.isEditing.value shouldBe false
            viewModel.isLoading.value shouldBe true

            advanceUntilIdle()
            viewModel.textValue shouldBe "old value"
            viewModel.isLoading.value shouldBe false
        }

        withCoroutinesShould("reflect external changes in the local value") {
            val viewModel = editableTextFieldModel(coroutineContext)
            advanceUntilIdle()
            viewModel.isEditing.value shouldBe false
            viewModel.textValue shouldBe "old value"

            serverValue.value = "new value"
            advanceUntilIdle()
            viewModel.isEditing.value shouldBe false
            viewModel.textValue shouldBe "new value"
        }

        withCoroutinesShould("start and apply edit to affect the remote value") {
            serverValue.value = "current value"
            val viewModel = editableTextFieldModel(coroutineContext)
            advanceUntilIdle()
            viewModel.startEdit()
            viewModel.isEditing.value shouldBe true

            viewModel.update("edited value")
            viewModel.textValue shouldBe "edited value"
            serverValue.value shouldBe "current value"
            numCallsOnApplyValue.value shouldBe 0

            viewModel.approveEdit()
            advanceUntilIdle()
            viewModel.isEditing.value shouldBe false
            viewModel.isLoading.value shouldBe false
            serverValue.value shouldBe "edited value"
            numCallsOnApplyValue.value shouldBe 1
        }

        withCoroutinesShould("start and cancel edit without it affecting the remote value") {
            serverValue.value = "current value"
            val viewModel = editableTextFieldModel(coroutineContext)
            advanceUntilIdle()
            viewModel.startEdit()
            viewModel.isEditing.value shouldBe true

            viewModel.update("edited value")
            viewModel.textValue shouldBe "edited value"
            serverValue.value shouldBe "current value"
            numCallsOnApplyValue.value shouldBe 0

            viewModel.cancelEdit()
            viewModel.isLoading.value shouldBe false
            advanceUntilIdle()
            viewModel.isEditing.value shouldBe false
            serverValue.value shouldBe "current value"
            numCallsOnApplyValue.value shouldBe 0
        }

        withCoroutinesShould("restore edit state on failure when applying value") {
            onApplyValue = {
                numCallsOnApplyValue.value += 1
                Result.failure<Exception>(Exception("test error"))
            }
            val viewModel = editableTextFieldModel(coroutineContext)
            advanceUntilIdle()
            viewModel.startEdit()
            viewModel.update("new value")
            numCallsOnApplyValue.value shouldBe 0
            viewModel.error.value shouldBe null

            viewModel.approveEdit()
            advanceUntilIdle()
            viewModel.isEditing.value shouldBe true
            viewModel.textValue shouldBe "new value"
            viewModel.error.value shouldBe "test error"
            viewModel.isLoading.value shouldBe false
            serverValue.value shouldBe "old value"
            numCallsOnApplyValue.value shouldBe 1
        }

        withCoroutinesShould("be loading while value is applied") {
            isHoldingUpOnApplyValue.value = true
            val viewModel = editableTextFieldModel(coroutineContext)
            advanceUntilIdle()
            viewModel.isLoading.value shouldBe false

            viewModel.startEdit()
            viewModel.update("new value")
            viewModel.approveEdit()
            advanceUntilIdle()
            viewModel.isLoading.value shouldBe true

            isHoldingUpOnApplyValue.value = false
            advanceUntilIdle()
            viewModel.isLoading.value shouldBe false
        }

        withCoroutinesShould("not apply changes while loading") {
            isHoldingUpOnApplyValue.value = true
            val viewModel = editableTextFieldModel(coroutineContext)
            advanceUntilIdle()

            viewModel.startEdit()
            viewModel.update("first edit")
            viewModel.approveEdit()
            advanceUntilIdle()
            numCallsOnApplyValue.value shouldBe 1

            viewModel.update("second edit")
            viewModel.approveEdit()
            advanceUntilIdle()
            numCallsOnApplyValue.value shouldBe 1
        }

        withCoroutinesShould("not apply changes if value doesn't differ") {
            serverValue.value = "same value"
            val viewModel = editableTextFieldModel(coroutineContext)
            advanceUntilIdle()
            viewModel.startEdit()
            viewModel.update("same value")
            viewModel.approveEdit()
            advanceUntilIdle()
            numCallsOnApplyValue.value shouldBe 0
            serverValue.value shouldBe "same value"
        }
    }

    private fun editableTextFieldModel(
        coroutineContext: CoroutineContext,
    ): ApprovableTextFieldViewModelImpl = ApprovableTextFieldViewModelImpl(
        coroutineScope = CoroutineScope(context = coroutineContext),
        onApplyChange = onApplyValue,
        serverValue = serverValue,
    )
}
