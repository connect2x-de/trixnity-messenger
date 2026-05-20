package de.connect2x.trixnity.messenger.compose.view.common.modifier

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import de.connect2x.trixnity.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.trixnity.messenger.compose.view.theme.messengerFocusIndicator

@Composable
fun Modifier.focusHighlighting(): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.focusable(true, interactionSource).focusHighlighting(interactionSource)
}

@Composable
fun Modifier.focusHighlighting(
    interactionSource: MutableInteractionSource,
    color: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = RectangleShape,
): Modifier =
    focusHighlighting(
        hasFocus = interactionSource.collectIsFocusedAsState().value,
        isFocusHighlightingActive = IsFocusHighlighting.current,
        borderWidth = MaterialTheme.messengerFocusIndicator.borderWidth,
        color = color,
        shape = shape,
    )

@Stable
fun Modifier.focusHighlighting(
    hasFocus: Boolean,
    isFocusHighlightingActive: Boolean,
    borderWidth: Dp,
    color: Color,
    shape: Shape = RectangleShape,
): Modifier {
    return if (!hasFocus || !isFocusHighlightingActive) this else border(borderWidth, color, shape)
}
