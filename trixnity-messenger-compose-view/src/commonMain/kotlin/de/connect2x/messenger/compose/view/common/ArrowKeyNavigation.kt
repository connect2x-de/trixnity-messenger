package de.connect2x.messenger.compose.view.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun Modifier.justArrowKeyNavigation(
    focusManager: FocusManager = LocalFocusManager.current
) = onKeyEvent { event ->
    when {
        event.type == KeyEventType.Companion.KeyUp && event.key == Key.Companion.DirectionUp ->
            focusManager.moveFocus(FocusDirection.Companion.Up)
        event.type == KeyEventType.Companion.KeyUp && event.key == Key.Companion.DirectionLeft ->
            focusManager.moveFocus(FocusDirection.Companion.Left)
        event.type == KeyEventType.Companion.KeyUp && event.key == Key.Companion.DirectionRight ->
            focusManager.moveFocus(FocusDirection.Companion.Right)
        event.type == KeyEventType.Companion.KeyUp && event.key == Key.Companion.DirectionDown ->
            focusManager.moveFocus(FocusDirection.Companion.Down)
        else -> false
    }
}
