package de.connect2x.messenger.compose.view.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.theme.components

@Composable
fun ThemedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    style: SurfaceStyle = MaterialTheme.components.dropdownMenu,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        content = content,
        shape = style.shape,
        containerColor = style.color,
        tonalElevation = style.tonalElevation,
        border = style.border,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxScope.ThemedExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    matchTextFieldWidth: Boolean = true,
    style: SurfaceStyle = MaterialTheme.components.dropdownMenu,
    content: @Composable ColumnScope.() -> Unit,
) {
    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        scrollState = scrollState,
        matchTextFieldWidth = matchTextFieldWidth,
        shape = style.shape,
        containerColor = style.color,
        tonalElevation = style.tonalElevation,
        border = style.border,
        shadowElevation = style.shadowElevation,
        content = content,
    )
}

@Immutable
data class DropdownMenuItemStyle(
    val colors: MenuItemColors,
    val contentPadding: PaddingValues,
    val focusedBorder: BorderStroke?,
) {
    companion object {
        @Composable
        fun default(
            colors: MenuItemColors = MenuDefaults.itemColors(),
            contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
            focusedBorder: BorderStroke? = null,
        ) = DropdownMenuItemStyle(
            colors = colors,
            contentPadding = contentPadding,
            focusedBorder = focusedBorder,
        )
    }
}

@Composable
fun ThemedDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    style: DropdownMenuItemStyle = MaterialTheme.components.dropdownMenuItem,
) {
    val hasFocus = interactionSource.collectIsFocusedAsState()
    val border = style.focusedBorder?.let { borderStroke ->
        if (hasFocus.value) Modifier.border(borderStroke) else Modifier
    } ?: Modifier

    DropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = modifier
            .buttonPointerModifier(enabled)
            .then(border),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = style.colors,
        contentPadding = style.contentPadding,
        interactionSource = interactionSource,
    )
}
