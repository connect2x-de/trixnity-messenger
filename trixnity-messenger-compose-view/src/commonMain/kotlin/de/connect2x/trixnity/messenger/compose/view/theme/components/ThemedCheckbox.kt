package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.theme.components

data class CheckboxStyle(val colors: CheckboxColors) {
    companion object {
        @Composable fun default(colors: CheckboxColors = CheckboxDefaults.colors()) = CheckboxStyle(colors = colors)
    }
}

@Composable
fun ThemedCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: CheckboxStyle = MaterialTheme.components.checkbox,
    interactionSource: MutableInteractionSource? = null,
) =
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = style.colors,
        interactionSource = interactionSource,
    )
