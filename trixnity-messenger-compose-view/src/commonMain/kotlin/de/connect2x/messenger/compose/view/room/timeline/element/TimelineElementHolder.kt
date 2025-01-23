package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.room.timeline.UnreadMessagesIndicator
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger { }

interface TimelineElementHolderView {
    @Composable
    fun create(
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    )
}

@Composable
fun TimelineElementHolder(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
) {
    DI.get<TimelineElementHolderView>().create(timelineElementHolderViewModel)
}

class TimelineElementHolderViewImpl : TimelineElementHolderView {
    @Composable
    override fun create(
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    ) {
        Column {
            when (timelineElementHolderViewModel) {
                is TimelineElementHolderViewModel -> {
                    val showUnreadMarker = timelineElementHolderViewModel.showUnreadMarker.collectAsState().value
                    val showLoadingIndicatorBefore =
                        timelineElementHolderViewModel.showLoadingIndicatorBefore.collectAsState().value
                    val showLoadingIndicatorAfter =
                        timelineElementHolderViewModel.showLoadingIndicatorAfter.collectAsState().value
                    
                    AnimatedVisibility(showLoadingIndicatorBefore) { LoadingSpinner() }
                    TimelineElementHolderSwitch(timelineElementHolderViewModel)
                    AnimatedVisibility(showUnreadMarker) { UnreadMessagesIndicator() }
                    AnimatedVisibility(showLoadingIndicatorAfter) { LoadingSpinner() }
                }

                is OutboxElementHolderViewModel -> {
                    TimelineElementHolderSwitch(timelineElementHolderViewModel)
                }
            }
        }
    }
}

@Composable
fun TimelineElementHolderSwitch(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
) {
    val element = timelineElementHolderViewModel.element.collectAsState().value

    when (element) {
        is TimelineElementViewModel.Message<*>, is TimelineElementViewModel.State<*> -> {
            TimelineElementSelector(timelineElementHolderViewModel, element)
        }

        TimelineElementViewModel.Empty, null -> {}
    }
}
