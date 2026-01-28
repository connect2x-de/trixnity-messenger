package de.connect2x.messenger.compose.view.theme.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RichTooltipColors
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.components
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
data class TooltipStyle(
    val contentPadding: PaddingValues,
    val caretShape: Shape?,
    val shape: Shape,
    val colors: RichTooltipColors,
    val tonalElevation: Dp,
    val shadowElevation: Dp,
    val actionStyle: TextStyle,
    val titleStyle: TextStyle,
    val textStyle: TextStyle,
    val longPressDelay: Duration,
    val hoverShowDelay: Duration,
    val hoverHideDelay: Duration,
) {
    companion object {
        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun default(
            contentPadding: PaddingValues = PaddingValues(0.dp),
            caretShape: Shape? = null,
            shape: Shape = TooltipDefaults.richTooltipContainerShape,
            colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
            tonalElevation: Dp = 0.dp,
            shadowElevation: Dp = 3.dp,
            actionStyle: TextStyle = MaterialTheme.typography.labelLarge,
            titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
            textStyle: TextStyle = MaterialTheme.typography.bodySmall,
            // See https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/view/ViewConfiguration.java
            // See https://learn.microsoft.com/en-us/dotnet/api/system.windows.forms.tooltip.automaticdelay?view=windowsdesktop-10.0#remarks
            // Android DEFAULT_LONG_PRESS_TIMEOUT = 0.4s
            longPressDelay: Duration = 400.milliseconds,
            // Android HOVER_TOOLTIP_SHOW_TIMEOUT = 0.5s
            // Windows AutoPopDelay = 0.5s
            hoverShowDelay: Duration = 500.milliseconds,
            // Windows ReshowDelay = 0.1s
            hoverHideDelay: Duration = 100.milliseconds,
        ) = TooltipStyle(
            contentPadding = contentPadding,
            caretShape = caretShape,
            shape = shape,
            colors = colors,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            actionStyle = actionStyle,
            titleStyle = titleStyle,
            textStyle = textStyle,
            longPressDelay = longPressDelay,
            hoverShowDelay = hoverShowDelay,
            hoverHideDelay = hoverHideDelay,
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
    caretShape = style.caretShape,
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
