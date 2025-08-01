package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.icons.HelpIcon
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.messengerFocusIndicator

internal data class RadioSettingOption(
    val text: String,
    val explanation: String? = null,
    val enabled: Boolean = true,
    val style: TextStyle? = null
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
internal fun <T> ColumnScope.RadioSetting(
    title: @Composable () -> Unit,
    options: Map<T, RadioSettingOption>,
    value: T,
    set: (T) -> Unit,
    additionalContent: (@Composable ColumnScope.() -> Unit)? = null,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Default.Settings,
) {
    MoreOptions(title = title, icon = icon, enabled = enabled) {
        for ((key, option) in options) {
            RadioSettingOption(option, key, value, set, enabled)
        }
        if (additionalContent != null) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
                additionalContent()
            }
        }
    }
}

@Composable
private fun <T> RadioSettingOption(
    option: RadioSettingOption,
    key: T,
    value: T,
    set: (T) -> Unit,
    enabled: Boolean,
) {
    val hasFocus = remember { mutableStateOf(false) }
    val focusedBorder =
        if (IsFocusHighlighting.current && hasFocus.value) {
            Modifier.border(
                width = MaterialTheme.messengerFocusIndicator.borderWidth,
                color = MaterialTheme.colorScheme.onBackground,
            )
        } else Modifier

    val (optionText, optionExplanation, optionEnabled, optionStyle) = option
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(10.dp)
            .onFocusChanged { hasFocus.value = it.hasFocus }
            .then(focusedBorder)
            .clickable { set(key) }
            .buttonPointerModifier(enabled)
    ) {
        if (optionExplanation != null) {
            HelpIcon(optionExplanation)
            Spacer(modifier = Modifier.width(10.dp))
        } else Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = optionText,
            style = optionStyle ?: LocalTextStyle.current,
            modifier = Modifier.weight(1.0f, fill = true)
        )
        RadioButton(
            selected = key == value,
            enabled = enabled && optionEnabled,
            onClick = { set(key) },
        )
    }
}
