package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RedactedMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel


interface MessageAndDateView {
    @Composable
    fun create(
        roomMessageViewModel: RoomMessageViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        onLongPress: (Offset) -> Unit,
    )
}

@Composable
fun MessageAndDate(
    roomMessageViewModel: RoomMessageViewModel,
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    onLongPress: (Offset) -> Unit,
) {
    DI.get<MessageAndDateView>().create(roomMessageViewModel, timelineElementHolderViewModel, onLongPress)
}

class MessageAndDateViewImpl : MessageAndDateView {
    @Composable
    override fun create(
        roomMessageViewModel: RoomMessageViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        onLongPress: (Offset) -> Unit
    ) {
        Layout(content = {
            MessageContent(
                roomMessageViewModel,
                timelineElementHolderViewModel,
                onLongPress
            )
            MessageDateText(roomMessageViewModel, timelineElementHolderViewModel)
        }) { elements, constraints ->
            val spacing = 10.dp.roundToPx()
            val message = elements[0].measure(constraints)
            val date = elements.getOrNull(1)?.measure(constraints)

            date?.let {
                if (message.width + spacing + date.width < constraints.maxWidth) {
                    // add extra padding to bottom that is missing otherwise
                    val height = message.height + 10.dp.roundToPx()
                    layout(
                        width = message.width + spacing + date.width,
                        height = height,
                    ) {
                        message.place(0, 0)
                        date.place(message.width + spacing, height - date.height)
                    }
                } else {
                    layout(
                        width = constraints.maxWidth,
                        height = message.height + date.height
                    ) {
                        message.place(0, 0)
                        date.place(constraints.maxWidth - date.width, message.height)
                    }
                }
            } ?: layout(
                message.width,
                message.height
            ) {
                message.place(0, 0)
            }
        }
    }
}

@Composable
fun MessageDateText(
    roomMessageViewModel: RoomMessageViewModel,
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel
) {
    val i18n = DI.get<I18nView>()
    if (roomMessageViewModel !is RedactedMessageViewModel) {
        roomMessageViewModel.formattedTime?.let {
            Row(
                modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (timelineElementHolderViewModel is TimelineElementHolderViewModel) {
                    val isReplaced = timelineElementHolderViewModel.isReplaced.collectAsState()
                    if (isReplaced.value)
                        Text(
                            i18n.messageBubbleEdited(),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.paddingFromBaseline(0.dp).padding(end = 2.dp),
                            maxLines = 1,
                        )
                }
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.paddingFromBaseline(0.dp),
                    maxLines = 1,
                )
                ReadMarker(roomMessageViewModel, timelineElementHolderViewModel)
            }
        }
        if (roomMessageViewModel.formattedTime == null) {
            Spacer(Modifier.defaultMinSize(minWidth = 45.dp))
        }
    }
}
