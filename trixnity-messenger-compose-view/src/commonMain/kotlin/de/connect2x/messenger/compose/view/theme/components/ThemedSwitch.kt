package de.connect2x.messenger.compose.view.theme.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.theme.components

data class SwitchStyle(
    val colors: SwitchColors,
) {
    companion object {
        @Composable
        fun default(
            colors: SwitchColors = SwitchDefaults.colors(),
        ) = SwitchStyle(
            colors,
        )
    }
}

@Composable
fun ThemedSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    style: SwitchStyle = MaterialTheme.components.switch,
    interactionSource: MutableInteractionSource? = null,
) = Switch(
    checked,
    onCheckedChange,
    modifier.buttonPointerModifier(enabled),
    thumbContent,
    enabled,
    style.colors,
    interactionSource,
)
