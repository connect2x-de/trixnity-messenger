package de.connect2x.messenger.compose.view.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager


@Composable
fun TabInTextField(canEnter: Boolean, onEnter: () -> Unit): Modifier {
    val focusManager = LocalFocusManager.current
    return Modifier.onPreviewKeyEvent {
        if (it.type == KeyEventType.KeyDown) {
            when {
                it.key == Key.Tab && it.isShiftPressed -> {
                    focusManager.moveFocus(FocusDirection.Previous)
                    true
                }

                it.key == Key.Tab -> {
                    focusManager.moveFocus(FocusDirection.Next)
                    true
                }

                it.key == Key.Enter -> {
                    if (canEnter) onEnter()
                    true
                }

                else -> false
            }
        } else {
            false
        }
    }
}
