package de.connect2x.messenger.compose.view.common.modifier

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.messengerFocusIndicator

@Composable
fun Modifier.focusHighlighting(
    interactionSource: MutableInteractionSource,
    color: Color = MaterialTheme.colorScheme.onSurface,
): Modifier {
    val hasFocus = interactionSource.collectIsFocusedAsState().value

    return this
        .then(
            if (IsFocusHighlighting.current && hasFocus) {
                Modifier.border(
                    width = MaterialTheme.messengerFocusIndicator.borderWidth,
                    color = color,
                )
            } else Modifier
        )
}
