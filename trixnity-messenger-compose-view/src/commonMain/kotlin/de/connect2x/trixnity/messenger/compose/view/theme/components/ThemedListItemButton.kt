package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.messengerFocusIndicator

@Composable
fun ThemedListItemButton(
    headlineContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    style: ListItemStyle = MaterialTheme.components.listItem,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
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
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        style = style,
        modifier =
            modifier
                .then(focusedBorder)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                )
                .semantics(mergeDescendants = true) { role = Role.Button }
                .buttonPointerModifier(enabled),
    )
}
