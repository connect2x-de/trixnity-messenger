package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.InfoPopup
import de.connect2x.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel

interface MessageInfoView {
    @Composable
    fun create(
        roomMessageViewModel: RoomMessageViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        modifier: Modifier
    )
}

@Composable
fun MessageInfo(
    roomMessageViewModel: RoomMessageViewModel,
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    modifier: Modifier = Modifier
) {
    DI.get<MessageInfoView>().create(roomMessageViewModel, timelineElementHolderViewModel, modifier)
}

class MessageInfoViewImpl : MessageInfoView {
    @Composable
    override fun create(
        roomMessageViewModel: RoomMessageViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        modifier: Modifier
    ) {
        if (timelineElementHolderViewModel !is TimelineElementHolderViewModel) {
            return
        }

        val focusRequester = remember { FocusRequester() }
        val infoOpenFlow = timelineElementHolderViewModel.infoOpen
        val infoOpen by infoOpenFlow.collectAsState()
        var readers by remember { mutableStateOf(listOf<String>()) }
        val reactions = timelineElementHolderViewModel.reactions.collectAsState().value

        LaunchedEffect(infoOpen) {
            if (infoOpen) {
                readers = timelineElementHolderViewModel.isReadBy()
            }
        }

        InfoPopup(
            isOpen = infoOpen,
            focusRequester = focusRequester,
            onDismiss = {
                timelineElementHolderViewModel.infoOpen.value = false
            },
            readers = readers,
            reactors = reactions.mapValues { (_, value) -> value.map { it.sender } }
        )
    }
}
