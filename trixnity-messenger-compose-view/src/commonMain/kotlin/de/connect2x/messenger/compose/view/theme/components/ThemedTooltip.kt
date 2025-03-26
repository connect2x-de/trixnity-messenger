package de.connect2x.messenger.compose.view.theme.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.RichTooltipColors
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.components

@OptIn(ExperimentalMaterial3Api::class)
data class TooltipStyle(
    val contentPadding: PaddingValues,
    val caretSize: DpSize,
    val shape: Shape,
    val colors: RichTooltipColors,
    val tonalElevation: Dp,
    val shadowElevation: Dp,
    val actionStyle: TextStyle,
    val titleStyle: TextStyle,
    val textStyle: TextStyle,
) {
    companion object {
        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun default(
            contentPadding: PaddingValues = PaddingValues(0.dp),
            caretSize: DpSize = DpSize.Unspecified,
            shape: Shape = TooltipDefaults.richTooltipContainerShape,
            colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
            tonalElevation: Dp = 0.dp,
            shadowElevation: Dp = 3.dp,
            actionStyle: TextStyle = MaterialTheme.typography.labelLarge,
            titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
            textStyle: TextStyle = MaterialTheme.typography.bodySmall,
        ) = TooltipStyle(
            contentPadding = contentPadding,
            caretSize = caretSize,
            shape = shape,
            colors = colors,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            actionStyle = actionStyle,
            titleStyle = titleStyle,
            textStyle = textStyle,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipScope.ThemedPlainTooltip(
    modifier: Modifier = Modifier,
    style: TooltipStyle = MaterialTheme.components.tooltip,
    content: @Composable () -> Unit
) = PlainTooltip(
    modifier = modifier,
    caretSize = style.caretSize,
    shape = style.shape,
    contentColor = style.colors.contentColor,
    containerColor = style.colors.containerColor,
    tonalElevation = style.tonalElevation,
    shadowElevation = style.shadowElevation,
) {
    Box(modifier = Modifier.padding(style.contentPadding)) {
        content()
    }
}
