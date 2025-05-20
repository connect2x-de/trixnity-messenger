package de.connect2x.messenger.compose.view.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.theme.components

@Immutable
data class ButtonStyle(
    val shape: Shape,
    val colors: ButtonColors,
    val elevation: ButtonElevation?,
    val enabledBorder: BorderStroke?,
    val disabledBorder: BorderStroke?,
    val contentPadding: PaddingValues,
    val textStyle: TextStyle?,
    val iconSize: Dp,
    val iconSpacing: Dp,
) {
    fun border(enabled: Boolean) = if (enabled) enabledBorder else disabledBorder

    companion object {
        @Composable
        fun text(
            shape: Shape = ButtonDefaults.textShape,
            colors: ButtonColors = ButtonDefaults.textButtonColors(),
            contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
            textStyle: TextStyle? = MaterialTheme.typography.labelLarge,
            iconSize: Dp = ButtonDefaults.IconSize,
            iconSpacing: Dp = ButtonDefaults.IconSpacing,
        ) = ButtonStyle(
            shape = shape,
            colors = colors,
            elevation = null,
            enabledBorder = null,
            disabledBorder = null,
            contentPadding = contentPadding,
            textStyle = textStyle,
            iconSize = iconSize,
            iconSpacing = iconSpacing,
        )

        @Composable
        fun outlined(
            shape: Shape = ButtonDefaults.outlinedShape,
            colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
            enabledBorder: BorderStroke? = ButtonDefaults.outlinedButtonBorder(true),
            disabledBorder: BorderStroke? = ButtonDefaults.outlinedButtonBorder(false),
            contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
            textStyle: TextStyle? = MaterialTheme.typography.labelLarge,
            iconSize: Dp = ButtonDefaults.IconSize,
            iconSpacing: Dp = ButtonDefaults.IconSpacing,
        ) = ButtonStyle(
            shape = shape,
            colors = colors,
            elevation = null,
            enabledBorder = enabledBorder,
            disabledBorder = disabledBorder,
            contentPadding = contentPadding,
            textStyle = textStyle,
            iconSize = iconSize,
            iconSpacing = iconSpacing,
        )

        @Composable
        fun filled(
            shape: Shape = ButtonDefaults.shape,
            colors: ButtonColors = ButtonDefaults.buttonColors(),
            elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
            contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
            textStyle: TextStyle? = MaterialTheme.typography.labelLarge,
            iconSize: Dp = ButtonDefaults.IconSize,
            iconSpacing: Dp = ButtonDefaults.IconSpacing,
        ) = ButtonStyle(
            shape = shape,
            colors = colors,
            elevation = elevation,
            enabledBorder = null,
            disabledBorder = null,
            contentPadding = contentPadding,
            textStyle = textStyle,
            iconSize = iconSize,
            iconSpacing = iconSpacing,
        )

        @Composable
        fun filledTonal(
            shape: Shape = ButtonDefaults.filledTonalShape,
            colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
            elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
            contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
            textStyle: TextStyle? = MaterialTheme.typography.labelLarge,
            iconSize: Dp = ButtonDefaults.IconSize,
            iconSpacing: Dp = ButtonDefaults.IconSpacing,
        ) = ButtonStyle(
            shape = shape,
            colors = colors,
            elevation = elevation,
            enabledBorder = null,
            disabledBorder = null,
            contentPadding = contentPadding,
            textStyle = textStyle,
            iconSize = iconSize,
            iconSpacing = iconSpacing,
        )

        @Composable
        fun elevated(
            shape: Shape = ButtonDefaults.elevatedShape,
            colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
            elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
            contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
            textStyle: TextStyle? = MaterialTheme.typography.labelLarge,
            iconSize: Dp = ButtonDefaults.IconSize,
            iconSpacing: Dp = ButtonDefaults.IconSpacing,
        ) = ButtonStyle(
            shape = shape,
            colors = colors,
            elevation = elevation,
            enabledBorder = null,
            disabledBorder = null,
            contentPadding = contentPadding,
            textStyle = textStyle,
            iconSize = iconSize,
            iconSpacing = iconSpacing,
        )
    }
}

@Composable
private fun ButtonColors.withContentColors() = copy(
    contentColor = contentColor.withContentColor(),
    disabledContentColor = disabledContentColor.withContentColor(enabled = false),
)

@Composable
fun ThemedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: ButtonStyle = MaterialTheme.components.secondaryButton,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val textStyle = LocalTextStyle.current.merge(style.textStyle)
    Button(
        onClick = onClick,
        modifier = modifier.buttonPointerModifier(enabled),
        enabled = enabled,
        shape = style.shape,
        colors = style.colors.withContentColors(),
        elevation = style.elevation,
        border = style.border(enabled),
        contentPadding = style.contentPadding,
        interactionSource = interactionSource,
    ) {
        CompositionLocalProvider(LocalTextStyle provides textStyle) {
            content()
        }
    }
}
