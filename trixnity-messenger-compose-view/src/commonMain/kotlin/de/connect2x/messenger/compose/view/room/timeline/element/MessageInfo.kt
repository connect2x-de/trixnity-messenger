package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.EmojiPopup
import de.connect2x.messenger.compose.view.common.InfoPopup
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

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
    @OptIn(ExperimentalCoroutinesApi::class)
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

        val readers by remember {
            // To avoid computing any readers while the popup is not open
            infoOpenFlow.flatMapLatest { isOpen ->
                if (isOpen) timelineElementHolderViewModel.readers
                else MutableStateFlow(emptyList())
            }
        }.collectAsState(emptyList())

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
