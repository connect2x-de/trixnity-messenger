package de.connect2x.trixnity.messenger.compose.view.common.modifier

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield


enum class RovingFocusDirection(internal val directions: List<FocusDirection>) {
    Vertical(listOf(FocusDirection.Up, FocusDirection.Down)),
    Horizontal(listOf(FocusDirection.Left, FocusDirection.Right)),
    Grid(listOf(FocusDirection.Up, FocusDirection.Down, FocusDirection.Left, FocusDirection.Right));
}

/**
 * The singletonFocusRequester is used to focus the item which has the singletonFocusRequester (see rovingFocusItem) and
 * the focus direction is Next or Previous. If isFocusedItemVisible is false it will additionally first start a new coroutine,
 * scroll to the item using scrollToFocusedItem and then focus it.
 *
 */
@Composable
fun Modifier.rovingFocusContainer(
    direction: RovingFocusDirection = RovingFocusDirection.Vertical,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    singletonFocusRequester: FocusRequester,
    isFocusedItemVisible: () -> Boolean = { true },
    scrollToFocusedItem: suspend () -> Unit = {},
): Modifier {
    val focusManager = LocalFocusManager.current
    val inputModeManager = LocalInputModeManager.current
    val moveFocus = remember(focusManager, inputModeManager) {
        { focusDirection: FocusDirection ->
            inputModeManager.requestInputMode(InputMode.Keyboard)
            focusManager.moveFocus(focusDirection)
        }
    }
    return this.moveFocusOnDirection(moveFocus, direction.directions)
        .focusProperties @ExperimentalComposeUiApi {
            enter = {
                if (it.isTab()) {
                    if (!isFocusedItemVisible()) {
                        coroutineScope.launch {
                            scrollToFocusedItem()
                            yield()
                            singletonFocusRequester.requestFocus(it)
                        }
                    } else {
                        singletonFocusRequester.requestFocus(it)
                    }
                }
                FocusRequester.Default
            }
        }
}

/**
 * singletonFocusRequester should be a focusRequester stored at the scope of your rovingFocusContainer,
 * which the rovingFocusContainer uses to switch focus. Additionally you may set a condition which item
 * has the requester using hasFocus (such a hasFocus={false}, if no item should have a focusRequester)
 */
fun Modifier.rovingFocusItem(
    isFocused: Boolean,
    onFocus: () -> Unit,
    singletonFocusRequester: FocusRequester,
    hasRequester: () -> Boolean = { isFocused }
): Modifier = this
    .focusProperties { onEnter = { if (!isFocused && requestedFocusDirection.isTab()) cancelFocusChange() } }
    .focusGroup()
    .onFocusChanged { if (it.isFocused) onFocus() }
    .run { if (hasRequester()) focusRequester(singletonFocusRequester) else this@run }

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
