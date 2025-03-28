package de.connect2x.messenger.compose.view.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.theme.components

@Immutable
sealed interface IconButtonStyle {
    data class Default(
        val size: Dp,
        val colors: IconToggleButtonColors,
    ) : IconButtonStyle

    data class Filled(
        val size: Dp,
        val shape: Shape,
        val colors: IconToggleButtonColors,
    ) : IconButtonStyle

    data class FilledTonal(
        val size: Dp,
        val shape: Shape,
        val colors: IconToggleButtonColors,
    ) : IconButtonStyle

    data class Outlined(
        val size: Dp,
        val shape: Shape,
        val colors: IconToggleButtonColors,
        val enabledBorder: BorderStroke?,
        val disabledBorder: BorderStroke?,
    ) : IconButtonStyle

    companion object {
        @Composable
        fun default(
            size: Dp = 40.dp,
            colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) = Default(
            size = size,
            colors = colors,
        )

        @Composable
        fun filled(
            size: Dp = 40.dp,
            shape: Shape = IconButtonDefaults.filledShape,
            colors: IconToggleButtonColors = IconButtonDefaults.filledIconToggleButtonColors(),
        ) = Filled(
            size = size,
            shape = shape,
            colors = colors,
        )

        @Composable
        fun filledTonal(
            size: Dp = 40.dp,
            shape: Shape = IconButtonDefaults.filledShape,
            colors: IconToggleButtonColors = IconButtonDefaults.filledTonalIconToggleButtonColors(),
        ) = Filled(
            size = size,
            shape = shape,
            colors = colors,
        )

        @Composable
        fun outlined(
            size: Dp = 40.dp,
            shape: Shape = IconButtonDefaults.outlinedShape,
            colors: IconToggleButtonColors = IconButtonDefaults.outlinedIconToggleButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            enabledBorder: BorderStroke? = IconButtonDefaults.outlinedIconButtonBorder(true),
            disabledBorder: BorderStroke? = IconButtonDefaults.outlinedIconButtonBorder(false),
        ) = Outlined(
            size = size,
            shape = shape,
            colors = colors,
            enabledBorder = enabledBorder,
            disabledBorder = disabledBorder,
        )
    }
}

@Composable
fun ThemedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: IconButtonStyle = MaterialTheme.components.commonIconButton,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    when (style) {
        is IconButtonStyle.Default ->
            IconButton(
                onClick = onClick,
                modifier = modifier.requiredSize(style.size).buttonPointerModifier(enabled),
                enabled = enabled,
                colors = IconButtonColors(
                    containerColor = style.colors.containerColor,
                    contentColor = style.colors.contentColor,
                    disabledContainerColor = style.colors.disabledContainerColor,
                    disabledContentColor = style.colors.disabledContentColor,
                ),
                interactionSource = interactionSource,
                content = content
            )
        is IconButtonStyle.Filled ->
            FilledIconButton(
                onClick = onClick,
                modifier = modifier.requiredSize(style.size).buttonPointerModifier(enabled),
                enabled = enabled,
                shape = style.shape,
                colors = IconButtonColors(
                    containerColor = style.colors.containerColor,
                    contentColor = style.colors.contentColor,
                    disabledContainerColor = style.colors.disabledContainerColor,
                    disabledContentColor = style.colors.disabledContentColor,
                ),
                interactionSource = interactionSource,
                content = content
            )
        is IconButtonStyle.FilledTonal ->
            FilledTonalIconButton(
                onClick = onClick,
                modifier = modifier.requiredSize(style.size).buttonPointerModifier(enabled),
                enabled = enabled,
                shape = style.shape,
                colors = IconButtonColors(
                    containerColor = style.colors.containerColor,
                    contentColor = style.colors.contentColor,
                    disabledContainerColor = style.colors.disabledContainerColor,
                    disabledContentColor = style.colors.disabledContentColor,
                ),
                interactionSource = interactionSource,
                content = content
            )
        is IconButtonStyle.Outlined ->
            OutlinedIconButton(
                onClick = onClick,
                modifier = modifier.requiredSize(style.size).buttonPointerModifier(enabled),
                enabled = enabled,
                shape = style.shape,
                colors = IconButtonColors(
                    containerColor = style.colors.containerColor,
                    contentColor = style.colors.contentColor,
                    disabledContainerColor = style.colors.disabledContainerColor,
                    disabledContentColor = style.colors.disabledContentColor,
                ),
                interactionSource = interactionSource,
                content = content
            )
    }
}

@Composable
fun ThemedIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: IconButtonStyle = MaterialTheme.components.commonIconButton,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    when (style) {
        is IconButtonStyle.Default ->
            IconToggleButton(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier.size(style.size).buttonPointerModifier(enabled),
                enabled = enabled,
                colors = style.colors,
                interactionSource = interactionSource,
                content = content
            )
        is IconButtonStyle.Filled ->
            FilledIconToggleButton(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier.size(style.size).buttonPointerModifier(enabled),
                enabled = enabled,
                colors = style.colors,
                interactionSource = interactionSource,
                content = content
            )
        is IconButtonStyle.FilledTonal ->
            FilledTonalIconToggleButton(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier.size(style.size).buttonPointerModifier(enabled),
                enabled = enabled,
                colors = style.colors,
                interactionSource = interactionSource,
                content = content
            )
        is IconButtonStyle.Outlined ->
            OutlinedIconToggleButton(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier.size(style.size).buttonPointerModifier(enabled),
                enabled = enabled,
                colors = style.colors,
                interactionSource = interactionSource,
                content = content
            )
    }
}
