package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import kotlinx.coroutines.flow.collectLatest

// the class is used as intermediate representation for updating text field states and to enforce maxLength invariant on
// the input
@ConsistentCopyVisibility
private data class SanitizedState private constructor(val text: String, val selection: TextRange) {
    companion object {
        operator fun invoke(text: String, selection: TextRange?, maxLength: Int): SanitizedState {
            val sanitizedText = text.take(maxLength)
            val sanitizedSelection =
                selection?.run { TextRange(start, end.coerceIn(0..sanitizedText.length)) } ?: TextRange.Zero
            return SanitizedState(sanitizedText, sanitizedSelection)
        }
    }
}

@Composable
fun TextFieldViewModel.collectAsTextFieldValueState(
    focusRequester: FocusRequester? = null
): MutableState<TextFieldValue> {
    val uiState = remember {
        val sanitizedState = value.sanitize(maxLength)
        val delegate = mutableStateOf(TextFieldValue(sanitizedState.text, sanitizedState.selection))
        object : MutableState<TextFieldValue> by delegate {
            override var value: TextFieldValue
                get() = delegate.value
                set(value) {
                    delegate.value = value.copy(value.text.take(maxLength))
                }
        }
    }

    return collectAndSync(
        uiState = uiState,
        toSanitizedState = { SanitizedState(value.text, value.selection, maxLength) },
        updateUiState = { newState -> value = TextFieldValue(newState.text, newState.selection) },
        focusRequester = focusRequester,
    )
}

@Composable
fun TextFieldViewModel.collectAsTextFieldState(focusRequester: FocusRequester? = null): TextFieldState {
    val uiState = rememberTextFieldState(value.sanitize(maxLength))

    return collectAndSync(
        uiState = uiState,
        toSanitizedState = { SanitizedState(text.toString(), selection, maxLength) },
        updateUiState = { newState ->
            edit {
                replace(0, length, newState.text)
                selection = newState.selection
            }
        },
        focusRequester = focusRequester,
    )
}

@Composable
private fun rememberTextFieldState(sanitizedState: SanitizedState): TextFieldState {
    return rememberTextFieldState(initialText = sanitizedState.text, initialSelection = sanitizedState.selection)
}

@Composable
private fun <T> TextFieldViewModel.collectAndSync(
    uiState: T,
    toSanitizedState: T.() -> SanitizedState,
    updateUiState: T.(SanitizedState) -> Unit,
    focusRequester: FocusRequester? = null,
): T {
    val uiEpoch = remember { mutableStateOf(0UL) }

    // UI -> VM
    LaunchedEffect(Unit) {
        snapshotFlow { uiState.toSanitizedState() }
            .collectLatest { sanitizedState ->
                uiEpoch.value++
                if (uiEpoch.value > value.epoch) {
                    update(sanitizedState.text, sanitizedState.selection.run { IntRange(start, end) }, uiEpoch.value)
                }
            }
    }

    // VM -> UI
    LaunchedEffect(Unit) {
        collect { vmState ->
            if (vmState.epoch > uiEpoch.value) {
                val newState = vmState.sanitize(maxLength)
                val currentState = uiState.toSanitizedState()

                if (newState == currentState) {
                    // If the state is the same, the UI -> VM sync won't run, so we must set the epoch directly
                    uiEpoch.value = vmState.epoch
                } else {
                    // If the state has changed, the UI -> VM sync will increment the epoch, so we need to compensate
                    // for that
                    uiEpoch.value = vmState.epoch - 1u
                    uiState.updateUiState(newState)
                }
                focusRequester?.requestFocus()
            }
        }
    }

    return uiState
}

private fun TextFieldViewModel.State.sanitize(maxLength: Int): SanitizedState {
    return SanitizedState(text, selection?.run { TextRange(first, last) }, maxLength)
}
