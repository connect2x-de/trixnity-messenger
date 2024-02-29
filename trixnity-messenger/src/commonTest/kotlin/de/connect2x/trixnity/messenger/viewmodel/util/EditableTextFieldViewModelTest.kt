package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.viewmodel.util.EditableTextFieldState.Edit
import de.connect2x.trixnity.messenger.viewmodel.util.EditableTextFieldState.Read
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.advanceUntilIdle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
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
            viewModel.state.value.shouldBeInstanceOf<Read>()
            viewModel.state.value.isEditing() shouldBe false
            viewModel.state.value.value shouldBe ""
            viewModel.isLoading.value shouldBe true

            advanceUntilIdle()
            viewModel.state.value.value shouldBe "old value"
            viewModel.isLoading.value shouldBe false
        }

        withCoroutinesShould("reflect external changes in the local value") {
            val viewModel = editableTextFieldModel(coroutineContext)
            advanceUntilIdle()
            viewModel.state.value.shouldBeInstanceOf<Read>()
            viewModel.state.value.isEditing() shouldBe false
            viewModel.state.value.value shouldBe "old value"

            serverValue.value = "new value"
            advanceUntilIdle()
            viewModel.state.value.shouldBeInstanceOf<Read>()
            viewModel.state.value.isEditing() shouldBe false
            viewModel.state.value.value shouldBe "new value"
        }

        withCoroutinesShould("start and apply edit to affect the remote value") {
            serverValue.value = "current value"
            val viewModel = editableTextFieldModel(coroutineContext)
            advanceUntilIdle()
            viewModel.startEdit()
            viewModel.state.value.shouldBeInstanceOf<Edit>()
            viewModel.state.value.isEditing() shouldBe true

            viewModel.state.value.setEdit("edited value")
            viewModel.state.value.value shouldBe "edited value"
            serverValue.value shouldBe "current value"
            numCallsOnApplyValue.value shouldBe 0

            viewModel.applyEdit()
            advanceUntilIdle()
            viewModel.state.value.shouldBeInstanceOf<Read>()
            viewModel.state.value.isEditing() shouldBe false
            viewModel.isLoading.value shouldBe false
            serverValue.value shouldBe "edited value"
            numCallsOnApplyValue.value shouldBe 1
        }

        withCoroutinesShould("start and cancel edit without it affecting the remote value") {
            serverValue.value = "current value"
            val viewModel = editableTextFieldModel(coroutineContext)
            advanceUntilIdle()
            viewModel.startEdit()
            viewModel.state.value.shouldBeInstanceOf<Edit>()
            viewModel.state.value.isEditing() shouldBe true

            viewModel.state.value.setEdit("edited value")
            viewModel.state.value.value shouldBe "edited value"
            serverValue.value shouldBe "current value"
            numCallsOnApplyValue.value shouldBe 0

            viewModel.cancelEdit()
            viewModel.isLoading.value shouldBe false
            advanceUntilIdle()
            viewModel.state.value.shouldBeInstanceOf<Read>()
            viewModel.state.value.isEditing() shouldBe false
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
            viewModel.state.value.setEdit("new value")
            numCallsOnApplyValue.value shouldBe 0
            viewModel.error.value shouldBe null

            viewModel.applyEdit()
            advanceUntilIdle()
            viewModel.state.value.shouldBeInstanceOf<Edit>()
            viewModel.state.value.isEditing() shouldBe true
            viewModel.state.value.value shouldBe "new value"
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
            viewModel.state.value.setEdit("new value")
            viewModel.applyEdit()
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
            viewModel.state.value.setEdit("first edit")
            viewModel.applyEdit()
            advanceUntilIdle()
            numCallsOnApplyValue.value shouldBe 1

            viewModel.state.value.setEdit("second edit")
            viewModel.applyEdit()
            advanceUntilIdle()
            numCallsOnApplyValue.value shouldBe 1
        }

        withCoroutinesShould("trim the value before applying") {
            val viewModel = editableTextFieldModel(coroutineContext)
            advanceUntilIdle()
            viewModel.startEdit()
            viewModel.state.value.setEdit("\n\t edited value \n\t")
            viewModel.applyEdit()
            advanceUntilIdle()
            numCallsOnApplyValue.value shouldBe 1
            serverValue.value shouldBe "edited value"
        }

        withCoroutinesShould("not apply changes if value doesn't differ") {
            serverValue.value = "same value"
            val viewModel = editableTextFieldModel(coroutineContext)
            advanceUntilIdle()
            viewModel.startEdit()
            viewModel.state.value.setEdit("\n\t same value \n\t")
            viewModel.applyEdit()
            advanceUntilIdle()
            numCallsOnApplyValue.value shouldBe 0
            serverValue.value shouldBe "same value"
        }
    }

    private fun editableTextFieldModel(
        coroutineContext: CoroutineContext,
    ): EditableTextFieldViewModelImpl = EditableTextFieldViewModelImpl(
            coroutineScope = CoroutineScope(context = coroutineContext),
            onApplyChange = onApplyValue,
            serverValue = serverValue,
        )
}
