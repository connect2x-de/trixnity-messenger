package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.icons.HelpIcon
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.messengerFocusIndicator
import de.connect2x.messenger.compose.view.util.RovingFocusContainer
import de.connect2x.messenger.compose.view.util.RovingFocusItem
import de.connect2x.messenger.compose.view.util.rovingFocusItem
import de.connect2x.messenger.compose.view.util.verticalRovingFocus

internal data class RadioSettingOption(
    val text: String,
    val explanation: String? = null,
    val enabled: Boolean = true,
    val style: TextStyle? = null
)

@Composable
internal fun <T: Any> ColumnScope.RadioSetting(
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
    val keys = remember(options) { options.keys.toList() }
    val defaultItem = options.keys.firstOrNull()
    MoreOptions(title = title, icon = icon, enabled = enabled) {
        RovingFocusContainer {
            Column(
                modifier = Modifier.verticalRovingFocus(
                    default = defaultItem,
                    scroll = {},
                    up = {
                        val currentItem = activeRef.value ?: defaultItem
                        val currentIndex = keys.indexOf(currentItem)
                        val nextIndex = currentIndex.minus(1).coerceIn(keys.indices)
                        keys[nextIndex]
                    },
                    down = {
                        val currentItem = activeRef.value ?: defaultItem
                        val currentIndex = keys.indexOf(currentItem)
                        val nextIndex = currentIndex.plus(1).coerceIn(keys.indices)
                        keys[nextIndex]
                    },
                )
            ) {
                for ((key, option) in options) {
                    RovingFocusItem(key, options.keys.first()) {
                        RadioSettingOption(option, key, value, set, enabled)
                    }
                }
            }
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
    val interactionSource = remember { MutableInteractionSource() }
    val focused = interactionSource.collectIsFocusedAsState()
    val focusedBorder =
        if (IsFocusHighlighting.current && focused.value) {
            Modifier.border(
                width = MaterialTheme.messengerFocusIndicator.borderWidth,
                color = MaterialTheme.colorScheme.onBackground,
            )
        } else Modifier

    val (optionText, optionExplanation, optionEnabled, optionStyle) = option
    ListItem(
        headlineContent = { Text(optionText, style = optionStyle ?: LocalTextStyle.current) },
        leadingContent = if (optionExplanation != null) {
            @Composable { HelpIcon(optionExplanation) }
        } else null,
        trailingContent = {
            RadioButton(
                selected = key == value,
                enabled = enabled && optionEnabled,
                interactionSource = interactionSource,
                onClick = null,
                modifier = Modifier.minimumInteractiveComponentSize(),
            )
        },
        modifier = Modifier
            .rovingFocusItem()
            .then(focusedBorder)
            .selectable(
                selected = key == value,
                onClick = { set(key) },
                enabled = enabled && optionEnabled,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = LocalIndication.current
            )
            .buttonPointerModifier(enabled),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
        ),
    )
}
