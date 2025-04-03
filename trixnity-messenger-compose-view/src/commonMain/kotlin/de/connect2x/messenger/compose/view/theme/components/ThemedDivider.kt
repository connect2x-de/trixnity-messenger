package de.connect2x.messenger.compose.view.theme.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import de.connect2x.messenger.compose.view.theme.components

@Immutable
data class DividerStyle(
    val thickness: Dp,
    val color: Color,
    val padding: Dp,
) {
    companion object {
        @Composable
        fun default(
            thickness: Dp = DividerDefaults.Thickness,
            color: Color = DividerDefaults.color,
            padding: Dp = Dp.Unspecified,
        ) = DividerStyle(
            thickness = thickness,
            color = color,
            padding = padding,
        )
    }
}

@Composable
fun ThemedHorizontalDivider(
    modifier: Modifier = Modifier,
    style: DividerStyle? = MaterialTheme.components.divider,
) {
    if (style != null) {
        HorizontalDivider(
            modifier = modifier.padding(vertical = style.padding),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color,
        )
    }
}

@Composable
fun ThemedVerticalDivider(
    modifier: Modifier = Modifier,
    style: DividerStyle? = MaterialTheme.components.divider,
) {
    if (style != null) {
        VerticalDivider(
            modifier = modifier.padding(horizontal = style.padding),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color,
        )
    }
}
