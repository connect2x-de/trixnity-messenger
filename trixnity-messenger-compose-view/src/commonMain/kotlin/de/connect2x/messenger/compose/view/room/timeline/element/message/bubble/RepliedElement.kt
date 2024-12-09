//package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble
//
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.size
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import de.connect2x.messenger.compose.view.DI
//import de.connect2x.messenger.compose.view.get
//import de.connect2x.messenger.compose.view.room.timeline.ReferencedMessagePill
//import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
//import de.connect2x.messenger.compose.view.theme.messengerColors
//import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
//
//@Composable
//fun RepliedElement(holder: BaseTimelineElementHolderViewModel) {
//    val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
//    val repliedElement = holder.repliedElement.collectAsState().value
//    val sender = repliedElement?.sender?.collectAsState()?.value
//    val element = repliedElement?.element?.collectAsState()?.value
//
//    if (repliedElement != null && sender != null && element != null) {
//        ReferencedMessagePill(
//            senderName = sender.name,
//            senderNameColor = MaterialTheme.messengerColors.getUserColor(sender.userId),
//            content = {
//                timelineElementViewSelector.createReplyInTimeline(element)
//            },
//        )
//        Spacer(Modifier.size(5.dp))
//    }
//}
