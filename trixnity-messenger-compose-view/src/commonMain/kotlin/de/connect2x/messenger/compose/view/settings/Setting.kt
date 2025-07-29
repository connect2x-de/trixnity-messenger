package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.IsA11yMode
import de.connect2x.messenger.compose.view.theme.components.ThemedSwitch
import de.connect2x.messenger.compose.view.theme.messengerFocusIndicator

@Composable
fun Setting(
    text: String,
    explanation: String? = null,
    value: Boolean = false,
    enabled: Boolean = true,
    toggle: (Boolean) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hasFocus = interactionSource.collectIsFocusedAsState().value
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
            .clickable(interactionSource = interactionSource, indication = null) {
                if (enabled) toggle(!value) // Only allow this if the setting is enabled
            }
            .focusable(interactionSource = interactionSource)
            .then(
                if (IsA11yMode.current && hasFocus) Modifier.border(
                    width = MaterialTheme.messengerFocusIndicator.borderWidth,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                else Modifier
            )
    ) {
        Column(Modifier.weight(1f, fill = true)) {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
            if (explanation != null) Text(text = explanation, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.size(5.dp))
        ThemedSwitch(
            checked = value,
            enabled = enabled,
            onCheckedChange = { toggle(it) },
        )
    }
}
