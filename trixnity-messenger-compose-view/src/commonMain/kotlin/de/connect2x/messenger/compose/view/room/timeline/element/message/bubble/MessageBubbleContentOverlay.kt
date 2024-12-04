package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.messengerColors

@Composable
fun BoxScope.MessageBubbleContentOverlay(
    hoverMessage: State<Boolean>,
    overlay: @Composable BoxScope.() -> Unit
) {
    AnimatedVisibility(
        hoverMessage.value,
        Modifier
            .align(Alignment.BottomStart)
            .padding(5.dp),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.messengerColors.metaDataPreviewBackground)
                .padding(6.dp)
        ) {
            overlay()
        }
    }
}
