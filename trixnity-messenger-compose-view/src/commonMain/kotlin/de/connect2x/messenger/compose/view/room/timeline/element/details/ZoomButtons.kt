package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView

@Composable
fun ZoomButtons(scale: MutableState<Float>, minScale: Float = 0.2f, maxScale: Float = 4.0f) {
    val i18n = DI.get<I18nView>()

    FileBasedDetailsHeaderButton(Icons.Outlined.ZoomIn, i18n.commonZoomIn()) {
        scale.value = (scale.value + 0.2f).coerceIn(minScale, maxScale)
    }
    FileBasedDetailsHeaderButton(Icons.Outlined.ZoomOut, i18n.commonZoomOut()) {
        scale.value = (scale.value - 0.2f).coerceIn(minScale, maxScale)
    }
}
