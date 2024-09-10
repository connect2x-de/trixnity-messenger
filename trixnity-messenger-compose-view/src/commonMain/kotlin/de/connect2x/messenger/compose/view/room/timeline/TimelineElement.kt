package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.room.timeline.element.MessageContainer
import de.connect2x.messenger.compose.view.room.timeline.element.UserVerification
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.HistoryVisibilityChangeStatusViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.MemberStatusViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.NullTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomAvatarChangeStatusViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomCreatedStatusViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomEncryptionEnabledViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomNameChangeStatusViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomTopicChangeStatusViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.UserVerificationViewModel
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger { }

interface TimelineElementView {
    @Composable
    fun create(
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        timelineViewModel: TimelineViewModel
    )
}

@Composable
fun TimelineElement(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    timelineViewModel: TimelineViewModel,
) {
    with(DI.current.get<TimelineElementView>()) { create(timelineElementHolderViewModel, timelineViewModel) }
}

class TimelineElementViewImpl : TimelineElementView {
    @Composable
    override fun create(
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        timelineViewModel: TimelineViewModel
    ) {
        Column {
            LoadingIndicatorBefore(timelineElementHolderViewModel)
            LeaveRoom(timelineElementHolderViewModel, timelineViewModel)
            UnreadMessagesIndicator(timelineElementHolderViewModel)
            DateChangeIndicator(timelineElementHolderViewModel)
            TimelineElementSwitch(timelineElementHolderViewModel)
            LoadingIndicatorAfter(timelineElementHolderViewModel)
        }
    }
}

@Composable
fun TimelineElementSwitch(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
) {
    val viewModel = timelineElementHolderViewModel.timelineElementViewModel.collectAsState()
    log.trace { "new timelineElementViewModel.viewModel for ${timelineElementHolderViewModel.key}" }
    when (val vm = viewModel.value) {
        is RoomMessageViewModel -> MessageContainer(vm, timelineElementHolderViewModel)
        is MemberStatusViewModel -> MemberChangeIndicator(vm)
        is RoomCreatedStatusViewModel -> RoomCreate(vm)
        is RoomAvatarChangeStatusViewModel -> RoomAvatarChange(vm)
        is RoomNameChangeStatusViewModel -> RoomNameChange(vm)
        is RoomTopicChangeStatusViewModel -> RoomTopicChange(vm)
        is HistoryVisibilityChangeStatusViewModel -> HistoryVisibilityChange(vm)
        is UserVerificationViewModel -> UserVerification(vm)
        is RoomEncryptionEnabledViewModel -> RoomEncryptionEnabled(vm)
        is NullTimelineElementViewModel -> Box {}
        else -> Box {}
    }
}