package de.connect2x.trixnity.messenger.compose.view.room.timeline.element

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.room.timeline.UnreadMessagesIndicator
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel


interface TimelineElementHolderView {
    @Composable
    fun create(
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        index: Int,
    )
}

@Composable
fun TimelineElementHolder(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    index: Int,
) {
    DI.get<TimelineElementHolderView>().create(timelineElementHolderViewModel, index)
}

class TimelineElementHolderViewImpl : TimelineElementHolderView {
    @Composable
    override fun create(
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        index: Int,
    ) {
        Column {
            when (timelineElementHolderViewModel) {
                is TimelineElementHolderViewModel -> {
                    val showUnreadMarker = timelineElementHolderViewModel.showUnreadMarker.collectAsState().value == true
                    val showLoadingIndicatorBefore =
                        timelineElementHolderViewModel.showLoadingIndicatorBefore.collectAsState().value
                    val showLoadingIndicatorAfter =
                        timelineElementHolderViewModel.showLoadingIndicatorAfter.collectAsState().value

                    AnimatedVisibility(showLoadingIndicatorBefore) { LoadingSpinner() }
                    TimelineElementHolderSwitch(timelineElementHolderViewModel, index)
                    AnimatedVisibility(showUnreadMarker) { UnreadMessagesIndicator() }
                    AnimatedVisibility(showLoadingIndicatorAfter) { LoadingSpinner() }
                }

                is OutboxElementHolderViewModel -> {
                    TimelineElementHolderSwitch(timelineElementHolderViewModel, index)
                }
            }
        }
    }
}

@Composable
fun TimelineElementHolderSwitch(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    index: Int,
) {
    when (val element = timelineElementHolderViewModel.element.collectAsState().value) {
        is TimelineElementViewModel.Message<*>, is TimelineElementViewModel.State<*> -> {
            TimelineElementSelector(timelineElementHolderViewModel, element, index)
        }

        TimelineElementViewModel.Empty, null -> {}
    }
}
