package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
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
import de.connect2x.messenger.compose.view.common.modifier.customClickable
import de.connect2x.messenger.compose.view.common.modifier.focusHighlighting
import de.connect2x.messenger.compose.view.theme.components.ThemedSwitch

@Composable
fun Setting(
    text: String,
    explanation: String? = null,
    value: Boolean = false,
    enabled: Boolean = true,
    toggle: (Boolean) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
            .customClickable(enabled = enabled) { toggle(!value) }
            .focusHighlighting(interactionSource)
    ) {
        Column(Modifier.weight(1f, fill = true)) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color =
                    if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            if (explanation != null) {
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
        Spacer(Modifier.size(5.dp))
        ThemedSwitch(
            checked = value,
            enabled = enabled,
            interactionSource = interactionSource,
            onCheckedChange = { toggle(it) },
        )
    }
}
