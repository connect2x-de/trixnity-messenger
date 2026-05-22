package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed interface ProgressIndicatorStyle {
    data class LinearProgressIndicatorStyle(
        val color: Color,
        val trackColor: Color,
        val strokeCap: StrokeCap,
        val gapSize: Dp,
        val padding: PaddingValues,
    ) : ProgressIndicatorStyle {
        companion object {
            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun default(
                color: Color = ProgressIndicatorDefaults.linearColor,
                trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
                strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                gapSize: Dp = ProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
                padding: PaddingValues = PaddingValues(0.dp),
            ) = LinearProgressIndicatorStyle(color, trackColor, strokeCap, gapSize, padding)
        }
    }

    data class CircularProgressIndicatorStyle(
        val size: Dp,
        val color: Color,
        val strokeWidth: Dp,
        val trackColor: Color,
        val strokeCap: StrokeCap,
        val padding: PaddingValues,
    ) : ProgressIndicatorStyle {
        companion object {
            @Composable
            fun default(
                size: Dp = 48.dp,
                color: Color = ProgressIndicatorDefaults.circularColor,
                strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
                trackColor: Color = ProgressIndicatorDefaults.circularDeterminateTrackColor,
                strokeCap: StrokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                padding: PaddingValues = PaddingValues(0.dp),
            ) = CircularProgressIndicatorStyle(size, color, strokeWidth, trackColor, strokeCap, padding)
        }
    }
}

@Composable
fun ThemedProgressIndicator(modifier: Modifier = Modifier, style: ProgressIndicatorStyle) {
    when (style) {
        is ProgressIndicatorStyle.LinearProgressIndicatorStyle ->
            LinearProgressIndicator(
                modifier.padding(style.padding),
                style.color,
                style.trackColor,
                style.strokeCap,
                style.gapSize,
            )

        is ProgressIndicatorStyle.CircularProgressIndicatorStyle ->
            CircularProgressIndicator(
                modifier = modifier.size(style.size - style.strokeWidth * 2).padding(style.padding),
                color = style.color,
                strokeWidth = style.strokeWidth,
                trackColor = style.trackColor,
                strokeCap = style.strokeCap,
            )
    }
}

@Composable
fun ThemedProgressIndicator(progress: () -> Float, modifier: Modifier = Modifier, style: ProgressIndicatorStyle) {
    when (style) {
        is ProgressIndicatorStyle.LinearProgressIndicatorStyle ->
            LinearProgressIndicator(
                progress = progress,
                modifier = modifier.padding(style.padding),
                color = style.color,
                trackColor = style.trackColor,
                strokeCap = style.strokeCap,
                gapSize = style.gapSize,
            )

        is ProgressIndicatorStyle.CircularProgressIndicatorStyle ->
            CircularProgressIndicator(
                progress = progress,
                modifier = modifier.size(style.size - style.strokeWidth * 2).padding(style.padding),
                color = style.color,
                strokeWidth = style.strokeWidth,
                trackColor = style.trackColor,
                strokeCap = style.strokeCap,
            )
    }
}
