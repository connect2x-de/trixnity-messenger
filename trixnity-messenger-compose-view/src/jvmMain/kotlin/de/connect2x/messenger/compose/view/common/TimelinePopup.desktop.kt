package de.connect2x.messenger.compose.view.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedSurface

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
                ThemedSurface(
                    style = MaterialTheme.components.popup,
                    modifier = Modifier.size(320.dp, 240.dp),
                ) {
                    content()
                }
            }
        }
    }
}
