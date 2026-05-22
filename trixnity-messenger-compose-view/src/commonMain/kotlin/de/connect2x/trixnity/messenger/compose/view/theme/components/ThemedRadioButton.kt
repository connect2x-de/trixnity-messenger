package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.theme.components

data class RadioButtonStyle(val colors: RadioButtonColors) {
    companion object {
        @Composable
        fun default(colors: RadioButtonColors = RadioButtonDefaults.colors()) = RadioButtonStyle(colors = colors)
    }
}

@Composable
fun ThemedRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: RadioButtonStyle = MaterialTheme.components.radioButton,
    interactionSource: MutableInteractionSource? = null,
) =
    RadioButton(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier.buttonPointerModifier(enabled),
        colors = style.colors,
        interactionSource = interactionSource,
    )
