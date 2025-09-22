package de.connect2x.messenger.compose.view.theme.components

import androidx.annotation.IntRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.theme.components

data class SliderStyle(
    val colors: SliderColors,
    val focusedBorder: BorderStroke?,
) {
    companion object {
        @Composable
        fun default(
            colors: SliderColors = SliderDefaults.colors(),
            focusedBorder: BorderStroke? = null,
        ) = SliderStyle(
            colors = colors,
            focusedBorder = focusedBorder,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    style: SliderStyle = MaterialTheme.components.slider,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val hasFocus = interactionSource.collectIsFocusedAsState().value
    val border = style.focusedBorder?.let { borderStroke ->
        if (enabled && hasFocus) Modifier.border(borderStroke)
        else Modifier
    } ?: Modifier

    Slider(
        value,
        onValueChange,
        modifier
            .then(border),
        enabled,
        valueRange,
        steps,
        onValueChangeFinished,
        style.colors,
        interactionSource,
    )
}

@Composable
@ExperimentalMaterial3Api
fun ThemedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    style: SliderStyle = MaterialTheme.components.slider,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    @IntRange(from = 0) steps: Int = 0,
    thumb: @Composable (SliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = interactionSource,
            colors = style.colors,
            enabled = enabled
        )
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        SliderDefaults.Track(colors = style.colors, enabled = enabled, sliderState = sliderState)
    },
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    val hasFocus = interactionSource.collectIsFocusedAsState().value
    val border = style.focusedBorder?.let { borderStroke ->
        if (enabled && hasFocus) Modifier.border(borderStroke)
        else Modifier
    } ?: Modifier

    Slider(
        value,
        onValueChange,
        modifier
            .then(border),
        enabled,
        onValueChangeFinished,
        style.colors,
        interactionSource,
        steps,
        thumb,
        track,
        valueRange,
    )
}
