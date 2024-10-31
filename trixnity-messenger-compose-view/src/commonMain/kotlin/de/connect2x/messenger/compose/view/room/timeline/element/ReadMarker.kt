package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.dp
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel

interface ReadMarkerView {
    @Composable
    fun create(
        roomMessageViewModel: RoomMessageViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    )
}

@Composable
fun ReadMarker(
    roomMessageViewModel: RoomMessageViewModel,
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
) {
    DI.get<ReadMarkerView>().create(roomMessageViewModel, timelineElementHolderViewModel)
}

class ReadMarkerViewImpl : ReadMarkerView {
    @Composable
    override fun create(
        roomMessageViewModel: RoomMessageViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel
    ) {
        val i18n = DI.get<I18nView>()
        if (timelineElementHolderViewModel is TimelineElementHolderViewModel) {
            val isRead = timelineElementHolderViewModel.isRead.collectAsState()
            val isByMe = roomMessageViewModel.isByMe
            Box(
                if (isByMe) Modifier
                    .size(MaterialTheme.typography.labelSmall.dp)
                    .padding(start = 2.dp)
                else Modifier
            ) {
                if (isRead.value) {
                    Icon(
                        Icons.Default.RemoveRedEye,
                        i18n.messageBubbleRead(),
                        Modifier.fillMaxSize(),
                    )

                } else {
                    Tooltip(tooltip = {
                        TooltipText { i18n.commonSent() }
                    }) {
                        Icon(
                            Icons.Default.Check,
                            i18n.messageBubbleRead(),
                            Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        } else Box(Modifier.size(MaterialTheme.typography.labelSmall.dp).padding(start = 2.dp))
    }
}
