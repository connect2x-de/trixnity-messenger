package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.foundation.layout.PaddingValues
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
import de.connect2x.trixnity.messenger.compose.view.theme.components

@Immutable
data class DividerStyle(val thickness: Dp, val color: Color, val padding: PaddingValues) {
    companion object {
        @Composable
        fun default(
            thickness: Dp = DividerDefaults.Thickness,
            color: Color = DividerDefaults.color,
            padding: PaddingValues = PaddingValues(),
        ) = DividerStyle(thickness = thickness, color = color, padding = padding)
    }
}

@Composable
fun ThemedHorizontalDivider(
    modifier: Modifier = Modifier,
    style: DividerStyle? = MaterialTheme.components.horizontalDivider,
) {
    if (style != null) {
        HorizontalDivider(
            modifier = modifier.padding(style.padding),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color,
        )
    }
}

@Composable
fun ThemedVerticalDivider(
    modifier: Modifier = Modifier,
    style: DividerStyle? = MaterialTheme.components.verticalDivider,
) {
    if (style != null) {
        VerticalDivider(
            modifier = modifier.padding(style.padding),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color,
        )
    }
}
