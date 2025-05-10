package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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

fun Modifier.zoomModifier(
    focusRequester: FocusRequester,
    canZoom: MutableState<Boolean>,
    zoom: MutableState<Float>,
    minScale: Float? = 0.2f,
    maxScale: Float? = 4f
): Modifier {
    return this.then(
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(pass = PointerEventPass.Final)
                        .changes
                        .forEach {
                            focusRequester.requestFocus() // otherwise, key events will be lost
                            if (canZoom.value) {
                                val delta = 0.1f * -it.scrollDelta.y
                                println("Changing zoom to $delta")
                                zoom.value = (zoom.value + delta).coerceIn(minScale, maxScale)
                            }
                        }
                }
            }
        }
    )
}

