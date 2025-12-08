package de.connect2x.messenger.compose.view.common.modifier

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager


enum class RovingFocusDirection(internal val directions: List<FocusDirection>) {
    Vertical(listOf(FocusDirection.Up, FocusDirection.Down)),
    Horizontal(listOf(FocusDirection.Left, FocusDirection.Right)),
    Grid(listOf(FocusDirection.Up, FocusDirection.Down, FocusDirection.Left, FocusDirection.Right));
}

@Composable
fun Modifier.rovingFocusContainer(direction: RovingFocusDirection = RovingFocusDirection.Vertical): Modifier {
    val focusManager = LocalFocusManager.current
    val inputModeManager = LocalInputModeManager.current
    val moveFocus = remember(focusManager, inputModeManager) {
        { focusDirection: FocusDirection ->
            inputModeManager.requestInputMode(InputMode.Keyboard)
            focusManager.moveFocus(focusDirection)
        }
    }
    return this.moveFocusOnDirection(moveFocus, direction.directions)
}

fun Modifier.rovingFocusItem(isFocused: Boolean, onFocus: () -> Unit): Modifier = this
    .focusProperties { onEnter = { if (!isFocused && requestedFocusDirection.isTab()) cancelFocusChange() } }
    .focusGroup()
    .onFocusChanged { if (it.isFocused) onFocus() }

private fun Modifier.moveFocusOnDirection(
    moveFocus: (FocusDirection) -> Boolean,
    directions: List<FocusDirection>,
): Modifier = this
    .onPreviewKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val pressedDirection = when (keyEvent.key) {
            Key.DirectionRight -> FocusDirection.Right
            Key.DirectionLeft -> FocusDirection.Left
            Key.DirectionDown -> FocusDirection.Down
            Key.DirectionUp -> FocusDirection.Up
            else -> return@onPreviewKeyEvent false
        }
        return@onPreviewKeyEvent directions.contains(pressedDirection) && moveFocus(pressedDirection)
    }
    .focusProperties { onExit = { if (directions.contains(requestedFocusDirection)) cancelFocusChange() } }
    .focusGroup()

private fun FocusDirection.isTab(): Boolean = this == FocusDirection.Next || this == FocusDirection.Previous
