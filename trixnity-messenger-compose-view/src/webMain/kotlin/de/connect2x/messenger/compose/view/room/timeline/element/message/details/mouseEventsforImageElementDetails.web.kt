package de.connect2x.messenger.compose.view.room.timeline.element.message.details

import androidx.compose.runtime.MutableState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent

private var oldPosition: Offset? = null // TODO include in composable

@OptIn(ExperimentalComposeUiApi::class)
actual fun mouseEventsForImageElementDetails(
    maxWidth: Float,
    maxHeight: Float,
    maxBoundsImage: Offset,
    scale: MutableState<Float>,
    move: MutableState<Offset>,
    xMin: MutableState<Float>,
    yMin: MutableState<Float>
): Modifier {
    val scaleFactorX = maxBoundsImage.x / maxWidth
    val scaleFactorY = maxBoundsImage.y / maxHeight
    return Modifier
        .onPointerEvent(PointerEventType.Scroll) {
            val event = it.changes.first()
            val scrollDelta = event.scrollDelta
            val newScale = scale.value - (0.1f * scrollDelta.y)
            scale.value = newScale.coerceIn(1f, 10f)
            val width = if (scaleFactorX * scale.value > 1) maxWidth else 0f
            val height = if (scaleFactorY * scale.value > 1) maxHeight else 0f
            xMin.value = width * (scale.value - 1) / 2
            yMin.value = height * (scale.value - 1) / 2
            event.consume()
        }.onPointerEvent(PointerEventType.Move) {
            val event = it.changes.first()
            val position = event.position
            if (oldPosition == null) {
                oldPosition = position
            }// to get the same amount of distance
            // when scaled, we have to move more pixels
            oldPosition?.let { // to get the same amount of distance
                // when scaled, we have to move more pixels
                val deltaX = it.x - position.x
                val deltaY = it.y - position.y
                val newMoveX =
                    move.value.x + (deltaX * scaleFactorX.coerceAtMost(1f) * scale.value) // when scaled, we have to move more pixels
                val newMoveY =
                    move.value.y + (deltaY * scaleFactorY.coerceAtMost(1f) * scale.value) // to get the same amount of distance
                move.value = Offset(
                    newMoveX.coerceIn(-xMin.value, xMin.value),
                    newMoveY.coerceIn(-yMin.value, yMin.value)
                )// to get the same amount of distance
                // when scaled, we have to move more pixels
                oldPosition = position
            }
            event.consume()
        }
}
