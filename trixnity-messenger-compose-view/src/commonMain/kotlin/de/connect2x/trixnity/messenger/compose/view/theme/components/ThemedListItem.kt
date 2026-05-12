package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import de.connect2x.trixnity.messenger.compose.view.theme.components

@Immutable
data class ListItemStyle(
    val colors: ListItemColors,
    val tonalElevation: Dp,
    val shadowElevation: Dp,
) {
    companion object {
        @Composable
        fun default(
            colors: ListItemColors = ListItemDefaults.colors(),
            tonalElevation: Dp = ListItemDefaults.Elevation,
            shadowElevation: Dp = ListItemDefaults.Elevation,
        ) = ListItemStyle(
            colors = colors,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
        )
    }
}

@Composable
fun ThemedListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    style: ListItemStyle = MaterialTheme.components.listItem,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) = ListItem(
    headlineContent = headlineContent,
    modifier = modifier,
    overlineContent = overlineContent,
    supportingContent = supportingContent,
    leadingContent = leadingContent,
    trailingContent = trailingContent,
    colors = style.colors,
    tonalElevation = style.tonalElevation,
    shadowElevation = style.shadowElevation,
)
