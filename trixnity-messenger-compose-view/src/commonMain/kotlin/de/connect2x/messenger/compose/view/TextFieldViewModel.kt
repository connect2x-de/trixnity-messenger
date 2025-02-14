package de.connect2x.messenger.compose.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel

@Composable
fun TextFieldViewModel.collectAsTextFieldValueState(focusRequester: FocusRequester? = null): MutableState<TextFieldValue> {
    val uiEpoch = remember { mutableStateOf(0UL) }
    val uiState = remember { mutableStateOf(value.toTextFieldValue()) }
    val uiStateValue = uiState.value
    LaunchedEffect(uiStateValue) {
        uiEpoch.value++
        update(uiStateValue.text, uiStateValue.selection.run { IntRange(start, end) }, uiEpoch.value)
    }
    LaunchedEffect(Unit) {
        collect { vmState ->
            if (vmState.epoch > uiEpoch.value) {
                uiEpoch.value = vmState.epoch - 1u // because setting uiState increases it again
                uiState.value = vmState.toTextFieldValue()
                focusRequester?.requestFocus()
            }
        }
    }

    return uiState
}

private fun TextFieldViewModel.State.toTextFieldValue() =
    TextFieldValue(
        text,
        selection?.run {
            TextRange(first, last.coerceIn(0..text.length))
        } ?: TextRange.Zero
    )
