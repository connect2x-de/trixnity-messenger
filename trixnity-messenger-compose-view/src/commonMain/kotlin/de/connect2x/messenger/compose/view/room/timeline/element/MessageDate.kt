package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RedactedMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomMessageViewModel

interface MessageDateView {
    @Composable
    fun ColumnScope.create(
        roomMessageViewModel: RoomMessageViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    )
}

@Composable
fun ColumnScope.MessageDate(
    roomMessageViewModel: RoomMessageViewModel,
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
) {
    with(DI.get<MessageDateView>()) { create(roomMessageViewModel, timelineElementHolderViewModel) }
}

class MessageDateViewImpl : MessageDateView {
    @Composable
    override fun ColumnScope.create(
        roomMessageViewModel: RoomMessageViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel
    ) {
        if (roomMessageViewModel !is RedactedMessageViewModel) {
            Row(Modifier.align(Alignment.End).padding(5.dp), verticalAlignment = Alignment.Bottom) {
                roomMessageViewModel.formattedTime?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.paddingFromBaseline(0.dp),
                            maxLines = 1,
                        )
                    }
                }
                ReadMarker(roomMessageViewModel, timelineElementHolderViewModel)
            }
            if (roomMessageViewModel.formattedTime == null) {
                Spacer(Modifier.defaultMinSize(minWidth = 45.dp))
            }
        }
    }
}
