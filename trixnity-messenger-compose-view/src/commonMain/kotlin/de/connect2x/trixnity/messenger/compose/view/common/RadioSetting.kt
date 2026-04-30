package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.common.icons.HelpIcon
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemRadioButton

internal data class RadioSettingOption(
    val text: String,
    val explanation: String? = null,
    val enabled: Boolean = true,
    val style: TextStyle? = null
)

@Composable
internal fun <T : Any> ColumnScope.RadioSetting(
    text: String,
    explanation: String? = null,
    options: Map<T, RadioSettingOption>,
    value: T,
    set: (T) -> Unit,
    additionalContent: (@Composable ColumnScope.() -> Unit)? = null,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Default.Settings,
) {
    RadioSetting(
        title = {
            Text(text, style = MaterialTheme.typography.titleSmall)
            if (explanation != null) Text(explanation, style = MaterialTheme.typography.labelSmall)
        },
        options = options,
        value = value,
        set = set,
        additionalContent = additionalContent,
        enabled = enabled,
        icon = icon
    )

}

@Composable
internal fun <T : Any> ColumnScope.RadioSetting(
    title: @Composable () -> Unit,
    options: Map<T, RadioSettingOption>,
    value: T,
    set: (T) -> Unit,
    additionalContent: (@Composable ColumnScope.() -> Unit)? = null,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Default.Settings,
) {
    var focusedItem by remember(value) { mutableStateOf(value) }

    ExpandableSection(heading = { title() }, icon = icon) {
        Column(modifier = Modifier.rovingFocusContainer(singletonFocusRequester = null)) {
            for ((key, option) in options) {
                val (optionText, optionExplanation, optionEnabled, optionStyle) = option
                ThemedListItemRadioButton(
                    style = MaterialTheme.components.settingsItem,
                    headlineContent = { Text(optionText, style = optionStyle ?: LocalTextStyle.current) },
                    leadingContent = if (optionExplanation != null) {
                        @Composable { HelpIcon(optionExplanation) }
                    } else null,
                    modifier = Modifier
                        .rovingFocusItem(
                            isFocused = focusedItem == key,
                            onFocus = { focusedItem = key },
                            singletonFocusRequester = null
                        )
                        .semantics(mergeDescendants = true) {
                            if (optionExplanation != null)
                                this.contentDescription = optionExplanation
                        },
                    enabled = enabled && optionEnabled,
                    selected = value == key,
                    onChange = { set(key) },
                )
            }
        }
    }
    if (additionalContent != null) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
            additionalContent()
        }
    }
}

