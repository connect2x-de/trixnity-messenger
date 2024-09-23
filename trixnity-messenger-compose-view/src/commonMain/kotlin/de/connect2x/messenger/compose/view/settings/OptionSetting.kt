package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import de.connect2x.messenger.compose.view.common.MoreOptions

internal data class OptionSettingOption(
    val text: String,
    val explanation: String? = null,
    val value: Boolean,
    val toggle: (Boolean) -> Unit,
    val enabled: Boolean = true,
)

// TODO delete?
@Composable
internal fun OptionSetting(
    text: String,
    explanation: String? = null,
    enabled: Boolean = true,
    options: List<OptionSettingOption>,
) {
    Text(text, style = MaterialTheme.typography.headlineSmall)
    if (explanation != null) Text(text, style = MaterialTheme.typography.labelSmall)
    for (option in options) {
        val (optionText, optionExplanation, optionValue, optionToggle, optionEnabled) = option
        Setting(
            text = optionText,
            explanation = optionExplanation,
            value = optionValue,
            enabled = enabled && optionEnabled,
            toggle = optionToggle
        )
    }
}

@Composable
internal fun ColumnScope.CollapsableOptionSetting(
    text: String,
    explanation: String? = null,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Default.Settings,
    options: List<OptionSettingOption>,
) {
    MoreOptions(title = {
        Text(text, style = MaterialTheme.typography.titleSmall)
        if (explanation != null) Text(explanation, style = MaterialTheme.typography.labelSmall)
    }, icon = icon, enabled = enabled) {
        for (option in options) {
            val (optionText, optionExplanation, optionValue, optionToggle, optionEnabled) = option
            Setting(
                text = optionText,
                explanation = optionExplanation,
                value = optionValue,
                enabled = enabled && optionEnabled,
                toggle = optionToggle
            )
        }
    }
}
