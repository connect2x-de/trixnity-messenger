package de.connect2x.messenger.compose.view.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

private fun downsampleAmplitudes(array: List<Float>, targetSize: Int): Array<Float> {
    val chunkSize = array.size.toFloat() / targetSize
    val output = Array(targetSize) { 0f }
    for (t in 0 until targetSize) {
        val start = (t * chunkSize).toInt()
        val end = ((t + 1) * chunkSize).coerceAtMost(array.size.toFloat())
        val slice = array.subList(start, end.toInt())
        output[t] = slice.maxOf { kotlin.math.abs(it) }
    }
    return output
}


private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun upsampleAmplitudes(array: Array<Float>, targetSize: Int): Array<Float> {
    val chunkSize = targetSize.toFloat() / array.size
    val output = Array(targetSize) { 0f }

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

@Composable
fun AudioWaveform(
    progress: Float,
    amplitudes: Array<Float>,
    width: Dp,
    height: Dp,
    amplitudeCount: Int = 100,
    modifier: Modifier = Modifier
) {
    val sampledAmplitudes = remember(amplitudes, amplitudeCount) {
        when {
            amplitudes.size > amplitudeCount -> downsampleAmplitudes(amplitudes.toList(), amplitudeCount)
            amplitudes.size < amplitudeCount -> upsampleAmplitudes(amplitudes, amplitudeCount)
            else -> amplitudes
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    Canvas(modifier = modifier.padding(5.dp).width(width).height(height)) {
        val barWidth = size.width / sampledAmplitudes.size

        sampledAmplitudes.forEachIndexed { index, amplitude ->
            val barHeight = amplitude * size.height
            val x = index * barWidth
            val y = size.height / 2 - barHeight / 2

            drawRoundRect(
                color = colorScheme.onBackground,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        }
    }
}
