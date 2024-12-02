package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.InfoPopup
import de.connect2x.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

interface MessageInfoView {
    @Composable
    fun create(
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        infoOpen: MutableState<Boolean>,
        modifier: Modifier
    )
}

@Composable
fun MessageInfo(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    infoOpen: MutableState<Boolean>,
    modifier: Modifier = Modifier
) {
    DI.get<MessageInfoView>().create(timelineElementHolderViewModel, infoOpen, modifier)
}

class MessageInfoViewImpl : MessageInfoView {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Composable
    override fun create(
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        infoOpen: MutableState<Boolean>,
        modifier: Modifier
    ) {
        if (timelineElementHolderViewModel !is TimelineElementHolderViewModel) {
            return
        }

        var readers = timelineElementHolderViewModel.isReadBy.collectAsState().value ?: emptyList()
        val focusRequester = remember { FocusRequester() }

        InfoPopup(
            isOpen = infoOpen.value,
            focusRequester = focusRequester,
            onDismiss = {
                infoOpen.value = false
            },
            readers = readers,
        )
    }
}
