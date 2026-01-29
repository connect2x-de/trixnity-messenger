package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.details

import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ZoomButtons(scale: MutableState<Float>, minScale: Float = 0.2f, maxScale: Float = 4.0f, enabled: Boolean = true) {
    val i18n = DI.get<I18nView>()

    FileBasedDetailsHeaderButton(Icons.Outlined.ZoomIn, i18n.commonZoomIn(), isEnabled = enabled) {
        scale.value = (scale.value + 0.2f).coerceIn(minScale, maxScale)
    }
    FileBasedDetailsHeaderButton(Icons.Outlined.ZoomOut, i18n.commonZoomOut(), isEnabled = enabled) {
        scale.value = (scale.value - 0.2f).coerceIn(minScale, maxScale)
    }
}

@Composable
fun ZoomButtons(onZoom: (factor: Float) -> Unit, enabled: Boolean = true) {
    val i18n = DI.get<I18nView>()

    FileBasedDetailsHeaderButton(Icons.Outlined.ZoomIn, i18n.commonZoomIn(), isEnabled = enabled) {
        onZoom(1.2f)
    }
    FileBasedDetailsHeaderButton(Icons.Outlined.ZoomOut, i18n.commonZoomOut(), isEnabled = enabled) {
        onZoom(0.8f)
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
                                zoom.value = (zoom.value + delta).coerceIn(minScale, maxScale)
                            }
                        }
                }
            }
        }
    )
}

/**
 * Overload to be used with a [TransformableState] that the zoom inputs are relayed to
 */
fun Modifier.zoomModifier(
    focusRequester: FocusRequester,
    canZoom: MutableState<Boolean>,
    state: TransformableState,
    scope: CoroutineScope
): Modifier {
    return this.then(
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(pass = PointerEventPass.Final)
                        .changes
                        .forEach {
                            if (it.scrollDelta.y.toInt() != 0 && canZoom.value) {
                                val delta = 0.1f * -it.scrollDelta.y
                                scope.launch {
                                    state.transform {
                                        val deltaMultiplier = 1 + delta
                                        this.transformBy(
                                            deltaMultiplier,
                                        )
                                    }
                                }
                            }
                        }
                }
            }
        }
    )
}

