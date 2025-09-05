package de.connect2x.messenger.compose.view.common.modifier

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.messengerFocusIndicator

@Composable
fun Modifier.focusHighlighting(
    interactionSource: MutableInteractionSource,
    color: Color = MaterialTheme.colorScheme.onSurface,
): Modifier {
    val hasFocus = interactionSource.collectIsFocusedAsState().value

    return this then focusHighlighting(
        hasFocus,
        IsFocusHighlighting.current,
        MaterialTheme.messengerFocusIndicator.borderWidth,
        color,
    )
}

@Stable
fun Modifier.focusHighlighting(
    hasFocus: Boolean,
    isFocusHighlightingActive: Boolean,
    borderWidth: Dp,
    color: Color,
): Modifier {
    return this
        .then(
            if (isFocusHighlightingActive && hasFocus) {
                Modifier.border(
                    width = borderWidth,
                    color = color,
                )
            } else Modifier
        )
}
