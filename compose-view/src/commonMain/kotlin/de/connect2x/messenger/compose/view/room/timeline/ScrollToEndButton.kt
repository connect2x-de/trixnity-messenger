package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel

interface ScrollToEndButtonView {
    @Composable
    fun BoxScope.create(timelineViewModel: TimelineViewModel, canScrollToEnd: Boolean)
}

@Composable
fun BoxScope.ScrollToEndButton(timelineViewModel: TimelineViewModel, canScrollToEnd: Boolean) {
    with(DI.current.get<ScrollToEndButtonView>()) { create(timelineViewModel, canScrollToEnd) }
}

class ScrollToEndButtonViewImpl : ScrollToEndButtonView {
    @Composable
    override fun BoxScope.create(timelineViewModel: TimelineViewModel, canScrollToEnd: Boolean) {
        AnimatedVisibility(
            visible = canScrollToEnd,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessVeryLow)),
            exit = fadeOut()
        ) {
            FloatingActionButton(
                onClick = { timelineViewModel.jumpToEndOfTimeline() },
                modifier = Modifier
                    .size(40.dp)
                    .buttonPointerModifier()
                    .indication(indication = null, interactionSource = MutableInteractionSource()),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "")
            }
        }
    }
}