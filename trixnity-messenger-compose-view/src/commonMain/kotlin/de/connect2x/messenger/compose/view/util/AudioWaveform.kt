package de.connect2x.messenger.compose.view.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil

private fun downsampleAmplitudes(array: List<Float>, targetSize: Int): List<Float> {
    val chunkSize = array.size.toFloat() / targetSize
    val output = MutableList(targetSize) { 0f }
    for (t in 0 until targetSize) {
        val start = (t * chunkSize).toInt()
        val end = ((t + 1) * chunkSize).coerceAtMost(array.size.toFloat())
        val slice = array.subList(start, end.toInt())
        output[t] = slice.maxOf { abs(it) }
    }
    return output
}


private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun upsampleAmplitudes(array: List<Float>, targetSize: Int): List<Float> {
    val chunkSize = targetSize.toFloat() / array.size
    val output = MutableList(targetSize) { 0f }

    for (i in array.indices) {
        val start = array[i]
        val end = if (i < array.lastIndex) array[i + 1] else array[i]

        val startIndex = ceil(i * chunkSize).toInt()
        val endIndex = ceil((i + 1) * chunkSize).toInt()

        for (j in startIndex until endIndex) {
            val t = (j - startIndex) / (endIndex - startIndex - 1f).coerceAtLeast(1f)
            output[j] = lerp(start, end, t)
        }
    }

    return output
}

/**
 * @param normalBarColor the color of the bar when not played
 * @param playedBarColor the color of the bar when played
 * @param borderRadius the round radius of the bar
 * @param barPaddingFactor the factor of a segment which is only padding between two bars (from 0.0 to 1.0)
 */
data class WaveformStyle(
    val normalBarColor: Color,
    val playedBarColor: Color,
    val borderRadius: Float,
    val barPaddingFactor: Float
)

@Composable
fun AudioWaveform(
    progress: Float,
    amplitudes: List<Float>,
    width: Dp,
    height: Dp,
    amplitudeCount: Int = 50,
    modifier: Modifier = Modifier,
    onPeek: (Float) -> Unit = {},
    colors: WaveformStyle = WaveformStyle(
        normalBarColor = Color.Black,
        playedBarColor = Color.White,
        borderRadius = 50f,
        barPaddingFactor = 0.15f
    ),
) {
    val sampledAmplitudes = remember(amplitudes, amplitudeCount) {
        when {
            amplitudes.size > amplitudeCount -> downsampleAmplitudes(amplitudes.toList(), amplitudeCount)
            amplitudes.size < amplitudeCount -> upsampleAmplitudes(amplitudes, amplitudeCount)
            else -> amplitudes
        }
    }

    val progressNormalized = progress.coerceIn(0.0f, 1.0f)
    val progressInIndex = progressNormalized * (sampledAmplitudes.size - 1).toFloat()
    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onPeek(newProgress)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    onPeek(newProgress)
                }
            }
    ) {
        val barWidth = size.width / sampledAmplitudes.size

        sampledAmplitudes.forEachIndexed { index, amplitude ->
            val barHeight = amplitude.coerceIn(0.025f, 1.0f) * size.height
            val indexAsFloat = index.toFloat()
            val color = when {
                indexAsFloat < progressInIndex -> {
                    if (indexAsFloat + 1 > progressInIndex) {
                        androidx.compose.ui.graphics.lerp(
                            colors.normalBarColor,
                            colors.playedBarColor,
                            (progressInIndex - indexAsFloat).coerceIn(0f, 1f)
                        )
                    } else {
                        colors.playedBarColor
                    }
                }
                else -> colors.normalBarColor
            }

            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(colors.borderRadius),
                topLeft = Offset(
                    x = index * barWidth,
                    y = size.height / 2 - barHeight / 2
                ),
                size = Size(
                    width = (barWidth * (1f - colors.barPaddingFactor)),
                    height = barHeight
                )
            )
        }
    }
}
