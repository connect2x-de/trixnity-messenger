package de.connect2x.messenger.compose.view.theme.components

import androidx.annotation.IntRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import de.connect2x.messenger.compose.view.theme.components

data class SliderStyle(
    val colors: SliderColors,
) {
    companion object {
        @Composable
        fun default(
            colors: SliderColors = SliderDefaults.colors(),
        ) = SliderStyle(
            colors = colors,
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
    focusedBorder: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val hasFocus = interactionSource.collectIsFocusedAsState().value
    val border = focusedBorder?.let { borderStroke ->
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
    focusedBorder: BorderStroke? = null,
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
    val border = focusedBorder?.let { borderStroke ->
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

@Composable
@ExperimentalMaterial3Api
fun ThemedSlider(
    state: SliderState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: SliderStyle = MaterialTheme.components.slider,
    focusedBorder: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumb: @Composable (SliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = interactionSource,
            colors = style.colors,
            enabled = enabled
        )
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        SliderDefaults.Track(colors = style.colors, enabled = enabled, sliderState = sliderState)
    }
) {
    val hasFocus = interactionSource.collectIsFocusedAsState().value
    val border = focusedBorder?.let { borderStroke ->
        if (enabled && hasFocus) Modifier.border(borderStroke)
        else Modifier
    } ?: Modifier
    Slider(
        state,
        modifier,
        enabled,
        style.colors,
        interactionSource,
        thumb,
        track,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemedRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    style: SliderStyle = MaterialTheme.components.slider,
    focusedBorder: BorderStroke? = null,
) {
    val hasFocus = remember { mutableStateOf(false) }
    val border = focusedBorder?.let { borderStroke ->
        if (enabled && hasFocus.value) Modifier.border(borderStroke)
        else Modifier
    } ?: Modifier
    RangeSlider(
        value,
        onValueChange,
        modifier
            .onFocusChanged { focusState -> hasFocus.value = focusState.isFocused }
            .then(border),
        enabled,
        valueRange,
        steps,
        onValueChangeFinished,
        style.colors,
    )
}

@Composable
@ExperimentalMaterial3Api
fun ThemedRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    style: SliderStyle = MaterialTheme.components.slider,
    focusedBorder: BorderStroke? = null,
    startInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    endInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    startThumb: @Composable (RangeSliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = startInteractionSource,
            colors = style.colors,
            enabled = enabled
        )
    },
    endThumb: @Composable (RangeSliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = endInteractionSource,
            colors = style.colors,
            enabled = enabled
        )
    },
    track: @Composable (RangeSliderState) -> Unit = { rangeSliderState ->
        SliderDefaults.Track(
            colors = style.colors,
            enabled = enabled,
            rangeSliderState = rangeSliderState
        )
    },
    @IntRange(from = 0) steps: Int = 0
) {
    val hasFocus = remember { mutableStateOf(false) }
    val border = focusedBorder?.let { borderStroke ->
        if (enabled && hasFocus.value) Modifier.border(borderStroke)
        else Modifier
    } ?: Modifier
    RangeSlider(
        value,
        onValueChange,
        modifier
            .onFocusChanged { focusState -> hasFocus.value = focusState.isFocused }
            .then(border),
        enabled,
        valueRange,
        onValueChangeFinished,
        style.colors,
        startInteractionSource,
        endInteractionSource,
        startThumb,
        endThumb,
        track,
    )
}

@Composable
@ExperimentalMaterial3Api
fun ThemedRangeSlider(
    state: RangeSliderState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: SliderStyle = MaterialTheme.components.slider,
    focusedBorder: BorderStroke? = null,
    startInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    endInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    startThumb: @Composable (RangeSliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = startInteractionSource,
            colors = style.colors,
            enabled = enabled
        )
    },
    endThumb: @Composable (RangeSliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = endInteractionSource,
            colors = style.colors,
            enabled = enabled
        )
    },
    track: @Composable (RangeSliderState) -> Unit = { rangeSliderState ->
        SliderDefaults.Track(
            colors = style.colors,
            enabled = enabled,
            rangeSliderState = rangeSliderState
        )
    }
) {
    val hasFocus = remember { mutableStateOf(false) }
    val border = focusedBorder?.let { borderStroke ->
        if (enabled && hasFocus.value) Modifier.border(borderStroke)
        else Modifier
    } ?: Modifier
    RangeSlider(
        state,
        modifier
            .onFocusChanged { focusState -> hasFocus.value = focusState.isFocused }
            .then(border),
        enabled,
        style.colors,
        startInteractionSource,
        endInteractionSource,
        startThumb,
        endThumb,
        track,
    )
}
