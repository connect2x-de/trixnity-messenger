package de.connect2x.messenger.compose.view.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
actual fun TimelinePopup(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier,
    isByMe: Boolean,
    content: @Composable () -> Unit
) {
    val expandedState = remember { MutableTransitionState(false) }
    expandedState.targetState = isOpen

    if (expandedState.currentState || expandedState.targetState || !expandedState.isIdle) {
        Popup(
            onDismissRequest = onDismiss,
            alignment = if (isByMe) Alignment.BottomEnd else Alignment.BottomStart,
            properties = PopupProperties(
                focusable = true,
            ),
        ) {
            AnimatedVisibility(
                visibleState = expandedState,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val focusManager = LocalFocusManager.current
                Surface(
                    Modifier.size(320.dp, 240.dp).onKeyEvent { event ->
                        when (event.key) {
                            Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
                            Key.DirectionLeft -> focusManager.moveFocus(FocusDirection.Left)
                            Key.DirectionRight -> focusManager.moveFocus(FocusDirection.Right)
                            Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                            else -> false
                        }
                    },
                    shadowElevation = 4.dp,
                    tonalElevation = 4.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    content()
                }
            }
        }
    }
}
