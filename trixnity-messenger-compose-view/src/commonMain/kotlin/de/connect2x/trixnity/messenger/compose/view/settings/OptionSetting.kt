package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import de.connect2x.trixnity.messenger.compose.view.common.ExpandableSection
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemSwitch

data class OptionSettingOption(
    val text: String,
    val explanation: String? = null,
    val value: Boolean,
    val toggle: (Boolean) -> Unit,
    val enabled: Boolean = true,
)

@Composable
fun ColumnScope.CollapsableOptionSetting(
    text: String,
    explanation: String? = null,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Default.Settings,
    options: List<OptionSettingOption>,
) {
    ExpandableSection(
        heading = {
            Text(text, style = MaterialTheme.typography.titleSmall)
            if (explanation != null) Text(explanation, style = MaterialTheme.typography.labelSmall)
        },
        icon = icon,
    ) {
        for (option in options) {
            val (optionText, optionExplanation, optionValue, optionToggle, optionEnabled) = option
            ThemedListItemSwitch(
                style = MaterialTheme.components.settingsItem,
                headlineContent = { Text(optionText) },
                supportingContent =
                    if (optionExplanation == null) null
                    else {
                        { Text(optionExplanation) }
                    },
                selected = optionValue,
                enabled = enabled && optionEnabled,
                onChange = optionToggle,
            )
        }
    }
}
