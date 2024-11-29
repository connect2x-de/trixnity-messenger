package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.MoreOptions
import de.connect2x.messenger.compose.view.common.deriveFromHue
import de.connect2x.messenger.compose.view.common.hue
import de.connect2x.messenger.compose.view.get

interface AppearanceSettingsColorView {
    @Composable
    fun ColumnScope.create(
        text: String,
        defaultColor: Color,
        color: Color,
        set: (Color) -> Unit
    )
}

@Composable
fun ColumnScope.AppearanceSettingsColor(
    text: String,
    defaultColor: Color,
    color: Color,
    set: (Color) -> Unit
) {
    with(DI.get<AppearanceSettingsColorView>()) { create(text, defaultColor, color, set) }
}

class AppearanceSettingsColorViewImpl : AppearanceSettingsColorView {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ColumnScope.create(text: String, defaultColor: Color, color: Color, set: (Color) -> Unit) {
        // This allows locally holding the color until the user lets go of the slider;
        // This prevents clogging the coroutine scheduler with redundant settings updates
        var newHue by remember { mutableStateOf(-1F) }

        // This function was intentionally picked over a state to reduce the lag while
        // adjusting the hue slider, since the preview/slider gets recomposed anyways.
        fun getCurrentHue() = if (newHue != -1F && newHue != color.hue) newHue else color.hue

        MoreOptions({
            Text(
                text = "${text}: ",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.width(5.dp))
            AppearanceSettingsColorPreview(color.deriveFromHue(getCurrentHue()))
        }) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = getCurrentHue(),
                    onValueChange = {
                        newHue = it
                    },
                    onValueChangeFinished = {
                        set(color.deriveFromHue(newHue))
                    },
                    valueRange = 0F..359F,
                    steps = 359,
                    modifier = Modifier.weight(1F),
                    track = { HueSliderTrack() }
                )
                Spacer(Modifier.width(10.dp))
                Button({
                    newHue = defaultColor.hue
                    set(defaultColor)
                }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HueSliderTrack(modifier: Modifier = Modifier.height(4.dp)) {
    var height by remember { mutableStateOf(0) }
    Canvas(modifier = modifier.fillMaxWidth()
        .onGloballyPositioned { height = it.size.height }) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val sliderLeft = Offset(0F, center.y)
        val sliderRight = Offset(size.width, center.y)
        val sliderStart = if (isRtl) sliderRight else sliderLeft
        val sliderEnd = if (isRtl) sliderLeft else sliderRight
        val sliderValueEnd = Offset(sliderStart.x + (sliderEnd.x - sliderStart.x), center.y)
        val sliderValueStart = Offset(sliderStart.x, center.y)
        drawLine(
            Brush.horizontalGradient(
                0F to Color.hsv(0F, 1F, 1F),
                1F / 6F to Color.hsv(60F, 1F, 1F),
                2F / 6F to Color.hsv(120F, 1F, 1F),
                3F / 6F to Color.hsv(180F, 1F, 1F),
                4F / 6F to Color.hsv(240F, 1F, 1F),
                5F / 6F to Color.hsv(300F, 1F, 1F),
                1F to Color.hsv(359F, 1F, 1F)
            ),
            sliderValueStart,
            sliderValueEnd,
            height.toFloat(),
            StrokeCap.Round
        )
    }
}
