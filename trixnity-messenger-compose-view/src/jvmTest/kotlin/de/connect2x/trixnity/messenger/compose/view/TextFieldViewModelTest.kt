package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalTestApi::class)
class TextFieldViewModelTest {
    @Test
    fun messageComposerSynchronizesTextSelectionAndClearing() = runComposeUiTest {
        val viewModel = TextFieldViewModelImpl(maxLength = 100)
        lateinit var uiState: TextFieldState
        setContent { uiState = viewModel.collectAsTextFieldState() }

        runOnIdle { viewModel.update("Draft", 5..5) }
        runOnIdle {
            assertEquals("Draft", uiState.text.toString())
            assertEquals(TextRange(5), uiState.selection)
            uiState.edit { selection = TextRange(2) }
        }
        runOnIdle { assertEquals(2..2, viewModel.selectionValue) }

        runOnIdle { viewModel.update("Hi @alice ", 10..10) }
        runOnIdle {
            assertEquals("Hi @alice ", uiState.text.toString())
            assertEquals(TextRange(10), uiState.selection)
            uiState.edit {
                append("hello")
                selection = TextRange(length)
            }
        }
        runOnIdle {
            assertEquals("Hi @alice hello", viewModel.textValue)
            assertEquals(15..15, viewModel.selectionValue)
            viewModel.update("")
        }
        runOnIdle {
            assertEquals("", uiState.text.toString())
            assertEquals(TextRange.Zero, uiState.selection)
            uiState.edit { append("Next message") }
        }
        runOnIdle { assertEquals("Next message", viewModel.textValue) }
    }

    @Test
    fun roomNameArrivingAfterCompositionIsDisplayed() = runComposeUiTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val serverValue = MutableStateFlow("")
        val viewModel = ApprovableTextFieldViewModelImpl(serverValue, 1_000, scope, { Result.success(Unit) })
        lateinit var uiState: MutableState<TextFieldValue>
        try {
            setContent { uiState = viewModel.collectAsTextFieldValueState() }
            runOnIdle { serverValue.value = "Room name" }
            waitUntil { viewModel.textValue == "Room name" }
            runOnIdle { assertEquals("Room name", uiState.value.text) }

            runOnIdle { serverValue.value = "Updated room name" }
            waitUntil { viewModel.textValue == "Updated room name" }
            runOnIdle { assertEquals("Updated room name", uiState.value.text) }

            runOnIdle {
                viewModel.startEdit()
                uiState.value = TextFieldValue("Draft", TextRange(5))
            }
            waitUntil { viewModel.textValue == "Draft" }
            runOnIdle { serverValue.value = "Remote change" }
            runOnIdle { assertEquals("Draft", uiState.value.text) }
            runOnIdle { viewModel.cancelEdit() }
            runOnIdle { assertEquals("Remote change", uiState.value.text) }
        } finally {
            scope.cancel()
        }
    }
}
