package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun RunningText(text: String, color: Color = Color.Unspecified, style: TextStyle = LocalTextStyle.current) {
    Text(text, modifier = Modifier.padding(bottom = 5.dp), color = color, style = style)
}

/**
 * A Composable to display one string as two text-components
 * for the purpose of cutting it in the middle in case of an overflow.
 * Only supports one line because of the row structure, more would lead to layout issues
 */
@Composable
fun OverflowingText(
    text: String,
    indexToCut: Int,
    overflow: TextOverflow,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    weightFirst: Float = 0.9f,
    weightSecond: Float = 1.0f
) {
    Row(modifier = modifier) {
        Text(
            text.substring(0, indexToCut),
            modifier = Modifier.weight(weightFirst, false),
            color = color,
            style = style,
            overflow = overflow,
            maxLines = 1
        )
        Text(
            text.substring(indexToCut, text.length),
            modifier = Modifier.weight(weightSecond, false),
            color = color,
            style = style,
            maxLines = 1
        )
    }
}
