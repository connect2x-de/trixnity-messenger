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
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.MessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

interface MessageInfoView {
    @Composable
    fun create(
        messageTimelineElementViewModel: MessageTimelineElementViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        modifier: Modifier
    )
}

@Composable
fun MessageInfo(
    messageTimelineElementViewModel: MessageTimelineElementViewModel,
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    modifier: Modifier = Modifier
) {
    DI.get<MessageInfoView>().create(messageTimelineElementViewModel, timelineElementHolderViewModel, modifier)
}

class MessageInfoViewImpl : MessageInfoView {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Composable
    override fun create(
        messageTimelineElementViewModel: MessageTimelineElementViewModel,
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
        )
    }
}
