package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun Modifier.inputFocusNavigation(): Modifier {
    val focusManager = LocalFocusManager.current

    return this then Modifier.onPreviewKeyEvent {
        if (it.key == Key.Tab && !it.isCtrlPressed && !it.isMetaPressed && !it.isAltPressed) {
            if (it.type == KeyEventType.KeyDown) {
                focusManager.moveFocus(
                    if (it.isShiftPressed) FocusDirection.Previous
                    else FocusDirection.Next,
                )
            }
            true
        } else {
            false
        }
    }
}
