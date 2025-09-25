package de.connect2x.messenger.compose.view.theme.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.Role
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.messengerFocusIndicator

@Composable
fun ThemedListItemSwitch(
    headlineContent: @Composable () -> Unit,
    selected: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    style: ListItemStyle = MaterialTheme.components.listItem,
    controlStyle: SwitchStyle = MaterialTheme.components.switch,
    thumbContent: (@Composable () -> Unit)? = null,
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

    ThemedListItem(
        headlineContent = headlineContent,
        leadingContent = leadingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        trailingContent = {
            ThemedSwitch(
                checked = selected,
                enabled = enabled,
                interactionSource = interactionSource,
                onCheckedChange = { onChange(it) },
                modifier = Modifier.minimumInteractiveComponentSize().focusProperties {
                    canFocus = false
                },
                style = controlStyle,
                thumbContent = thumbContent,
            )
        },
        style = style,
        modifier = modifier
            .then(focusedBorder)
            .selectable(
                selected = selected,
                onClick = { onChange(!selected) },
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ).buttonPointerModifier(enabled),
    )
}
