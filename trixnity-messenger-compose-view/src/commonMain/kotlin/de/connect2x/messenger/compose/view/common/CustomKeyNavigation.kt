package de.connect2x.messenger.compose.view.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
fun Modifier.customKeyNavigation(
    focusManager: FocusManager = LocalFocusManager.current
): Modifier = onKeyEvent { event ->
    when {
        event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp ->
            focusManager.moveFocus(FocusDirection.Up)

        event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft ->
            focusManager.moveFocus(FocusDirection.Left)

        event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight ->
            focusManager.moveFocus(FocusDirection.Right)

        event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown ->
            focusManager.moveFocus(FocusDirection.Down)

        else -> false
    }
}
