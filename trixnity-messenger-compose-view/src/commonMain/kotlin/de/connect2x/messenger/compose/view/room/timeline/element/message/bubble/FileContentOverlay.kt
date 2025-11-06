package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.messengerColors


@Composable
fun BoxScope.FileContentOverlay(
    hoverMessage: State<Boolean>,
    overlay: (@Composable BoxScope.() -> Unit)?,
) {
    overlay?.let {
        val boxAlpha: Float by animateFloatAsState(
            targetValue = if (hoverMessage.value) 1f else 0f,
            animationSpec = tween(
                durationMillis = 500,
                easing = LinearEasing,
            )
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .alpha(boxAlpha)
                .padding(5.dp)
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.messengerColors.metaDataPreviewBackground)
                    .padding(6.dp)
            ) {
                it()
            }
        }
    }
}
