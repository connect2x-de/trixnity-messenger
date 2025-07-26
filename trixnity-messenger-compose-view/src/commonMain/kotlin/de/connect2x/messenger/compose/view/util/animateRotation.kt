package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent

@Stable
fun Modifier.animateRotation(rotation: State<Float>) = drawWithContent {
    with (drawContext) {
        canvas.save()
        try {
            transform.rotate(rotation.value)
            drawContent()
        } finally {
            canvas.restore()
        }
    }
}
