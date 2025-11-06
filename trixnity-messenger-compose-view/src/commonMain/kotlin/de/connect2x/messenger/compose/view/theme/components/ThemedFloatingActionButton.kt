package de.connect2x.messenger.compose.view.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.theme.components

@Immutable
data class FloatingActionButtonStyle(
    val size: Dp,
    val shape: Shape,
    val containerColor: Color,
    val contentColor: Color,
    val elevation: FloatingActionButtonElevation,
    val focusedBorder: BorderStroke?,
) {
    companion object {
        @Composable
        fun default(
            size: Dp = 56.dp,
            shape: Shape = FloatingActionButtonDefaults.shape,
            containerColor: Color = FloatingActionButtonDefaults.containerColor,
            contentColor: Color = contentColorFor(containerColor),
            elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
            focusedBorder: BorderStroke? = null,
        ) = FloatingActionButtonStyle(
            size = size,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = elevation,
            focusedBorder = focusedBorder,
        )
    }
}

@Composable
fun ThemedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    enabled: Boolean = true,
    style: FloatingActionButtonStyle =
        if (enabled) MaterialTheme.components.floatingActionButton
        else MaterialTheme.components.floatingActionButtonDisabled,
    interactionSource: MutableInteractionSource? = null,
) {
    val hasFocus = remember { mutableStateOf(false) }
    val border = style.focusedBorder?.let { borderStroke ->
        if (enabled && hasFocus.value) Modifier.border(borderStroke, shape = style.shape)
        else Modifier
    } ?: Modifier

    Tooltip(tooltip = text, enabled = !expanded) {
        ExtendedFloatingActionButton(
            text = text,
            icon = icon,
            onClick = if (enabled) onClick else {
                {}
            },
            modifier = modifier
                .buttonPointerModifier(enabled = enabled)
                .onFocusEvent { focusState -> hasFocus.value = focusState.isFocused }
                .then(border),
            expanded = expanded,
            shape = style.shape,
            containerColor = style.containerColor,
            contentColor = style.contentColor.withContentColor(enabled),
            elevation = style.elevation,
            interactionSource = interactionSource,
        )
    }
}
