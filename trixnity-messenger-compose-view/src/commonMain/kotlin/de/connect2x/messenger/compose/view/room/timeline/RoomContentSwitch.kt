package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel

@Composable
fun RoomContentSwitch(
    stack: Value<ChildStack<*, TimelineRouter.Wrapper>>,
    showSettingsButton: Boolean,
) {
    Children(
        stack = stack,
        animation = stackAnimation(fade())
    ) {
        when (val child = it.instance) {
            is TimelineRouter.Wrapper.View -> RoomContent(child.viewModel, showSettingsButton)
            is TimelineRouter.Wrapper.None -> Box {}
        }.let {}
    }
}

@Composable
fun RoomContent(
    timelineViewModel: TimelineViewModel,
    showSettingsButton: Boolean = true
) {
    Column(Modifier.fillMaxSize()) {
        RoomHeader(timelineViewModel.roomHeaderViewModel, showSettingsButton)
        AttachmentTimelineSwitch(timelineViewModel)
    }
}

@Composable
fun AttachmentTimelineSwitch(timelineViewModel: TimelineViewModel) {
    Children(
        stack = timelineViewModel.sendAttachmentStack,
        animation = stackAnimation(fade()),
    ) {
        when (val child = it.instance) {
            is TimelineViewModel.Wrapper.View -> SendAttachment(child.viewModel)
            is TimelineViewModel.Wrapper.None -> {
                Column {
                    Timeline(timelineViewModel)
                    TypingIndicator(timelineViewModel)
                    InputArea(timelineViewModel.inputAreaViewModel)
                }
            }
        }.let {}
    }
}