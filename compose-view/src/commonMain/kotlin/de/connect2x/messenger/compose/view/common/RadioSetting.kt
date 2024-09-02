package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.common.icons.HelpIcon

internal data class RadioSettingOption(
    val text: String,
    val explanation: String? = null,
    val enabled: Boolean = true,
)

@Composable
internal fun <T> ColumnScope.RadioSetting(
    text: String,
    explanation: String? = null,
    options: Map<T, RadioSettingOption>,
    value: T,
    set: (T) -> Unit,
    additionalContent: (@Composable ColumnScope.() -> Unit)? = null,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Default.Settings,
) {
    MoreOptions(title = {
        Text(text, style = MaterialTheme.typography.titleSmall)
        if (explanation != null) Text(explanation, style = MaterialTheme.typography.labelSmall)
    }, icon = icon, enabled = enabled) {
        for ((key, option) in options) {
            val (optionText, optionExplanation, optionEnabled) = option
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(10.dp).clickable { set(key) }
            ) {
                if (optionExplanation != null) {
                    HelpIcon(optionExplanation)
                    Spacer(modifier = Modifier.width(10.dp))
                } else Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = optionText,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1.0f, fill = true)
                )
                RadioButton(
                    selected = key == value,
                    enabled = enabled && optionEnabled,
                    onClick = { set(key) },
                )
            }
        }
        if (additionalContent != null) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
                additionalContent()
            }
        }
    }
}
