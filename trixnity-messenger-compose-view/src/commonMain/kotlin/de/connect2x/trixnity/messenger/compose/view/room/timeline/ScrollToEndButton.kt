package de.connect2x.trixnity.messenger.compose.view.room.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedFloatingActionButton
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel

interface ScrollToEndButtonView {
    @Composable fun BoxScope.create(timelineViewModel: TimelineViewModel, canScrollToEnd: State<Boolean>)
}

@Composable
fun BoxScope.ScrollToEndButton(timelineViewModel: TimelineViewModel, canScrollToEnd: State<Boolean>) {
    with(DI.get<ScrollToEndButtonView>()) { create(timelineViewModel, canScrollToEnd) }
}

class ScrollToEndButtonViewImpl : ScrollToEndButtonView {
    @Composable
    override fun BoxScope.create(timelineViewModel: TimelineViewModel, canScrollToEnd: State<Boolean>) {
        val unreadCount = timelineViewModel.unreadCount.collectAsState()
        val i18n = DI.get<I18nView>()

        BadgedBox(
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            badge = {
                AnimatedVisibility(
                    visible = canScrollToEnd.value,
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessVeryLow)),
                    exit = fadeOut(),
                ) {
                    unreadCount.value?.let { unreadCount ->
                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) { Text(unreadCount) }
                    }
                }
            },
        ) {
            AnimatedVisibility(
                visible = canScrollToEnd.value,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessVeryLow)),
                exit = fadeOut(),
            ) {
                ThemedFloatingActionButton(
                    onClick = { timelineViewModel.jumpToEndOfTimeline() },
                    text = { Text(i18n.timelineJumpToEnd()) },
                    icon = { Icon(Icons.Default.KeyboardArrowDown, i18n.timelineJumpToEnd()) },
                )
            }
        }
    }
}
