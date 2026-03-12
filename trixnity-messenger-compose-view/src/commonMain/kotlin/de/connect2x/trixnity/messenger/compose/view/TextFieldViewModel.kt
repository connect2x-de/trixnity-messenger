package de.connect2x.trixnity.messenger.compose.view

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
    val uiState = remember {
        val delegate = mutableStateOf(value.toTextFieldValue(maxLength))
        object : MutableState<TextFieldValue> by delegate {
            override var value: TextFieldValue
                get() = delegate.value
                set(value) {
                    delegate.value = value.copy(value.text.take(maxLength))
                }
        }
    }
    val uiStateValue = uiState.value

    // UI -> VM sync
    LaunchedEffect(uiStateValue) {
        uiEpoch.value++
        if (uiEpoch.value > value.epoch) {
            println("TextField: Update (UI ->) ViewModel TextField to $uiStateValue")
            update(uiStateValue.text, uiStateValue.selection.run { IntRange(start, end) }, uiEpoch.value)
        }
    }

    // VM -> UI sync
    LaunchedEffect(Unit) {
        collect { vmState ->
            if (vmState.epoch > uiEpoch.value) {
                val newState = vmState.toTextFieldValue(maxLength)

                if (newState != uiState.value) {
                    // If the state has changed, the UI -> VM sync will increment the epoch, so we need to compensate for that
                    uiEpoch.value = vmState.epoch - 1u
                    uiState.value = newState
                    println("TextField: Update (ViewModel ->) UI TextField to $newState")
                } else {
                    // If the state is the same, the UI -> VM sync won't run, so we must set the epoch directly
                    uiEpoch.value = vmState.epoch
                }
                focusRequester?.requestFocus()
            }
        }
    }

    return uiState
}

private fun TextFieldViewModel.State.toTextFieldValue(maxLength: Int): TextFieldValue {
    return TextFieldValue(
        text.take(maxLength),
        selection?.run {
            TextRange(first, last.coerceIn(0..text.length))
        } ?: TextRange.Zero
    )
}
