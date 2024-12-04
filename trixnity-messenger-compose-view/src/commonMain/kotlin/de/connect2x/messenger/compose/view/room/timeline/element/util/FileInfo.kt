package de.connect2x.messenger.compose.view.room.timeline.element.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import de.connect2x.messenger.compose.view.common.OverflowingText
import kotlin.math.min

@Composable
fun OverflowingFileInfo(
    text: String,
    modifier: Modifier,
    color: Color = Color.Unspecified,
) {
    val indexOfLastPeriod = text.lastIndexOf('.')
    val indexToCut = min(
        if (indexOfLastPeriod > -1) indexOfLastPeriod else text.length,
        text.length,
    )
    OverflowingText(
        text,
        indexToCut,
        TextOverflow.Ellipsis,
        modifier = modifier,
        color = color,
    )
}
