package de.connect2x.messenger.compose.view.files

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

actual fun mouseEventsForImageOverlay(
    maxWidth: Float,
    maxHeight: Float,
    maxBoundsImage: Offset,
    scale: MutableState<Float>,
    move: MutableState<Offset>,
    xMin: MutableState<Float>,
    yMin: MutableState<Float>,
): Modifier {
    // do nothing in Android
    return Modifier
}
