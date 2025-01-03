//package de.connect2x.messenger.previews
//
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.tooling.preview.Preview
//import de.connect2x.messenger.compose.view.room.timeline.ImageReplyDefault
//import de.connect2x.messenger.compose.view.room.timeline.ReferencedMessagePill
//import de.connect2x.messenger.compose.view.room.timeline.ReplyToPill
//import de.connect2x.messenger.compose.view.room.timeline.TextReply
//import de.connect2x.messenger.previews.util.InitMessengerPreview
//import de.connect2x.trixnity.messenger.viewmodel.room.timeline.PreviewReplyToViewModel
//
//@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
//@Composable
//private fun ReplyToPillPreview() {
//    InitMessengerPreview {
//        ReplyToPill(PreviewReplyToViewModel()) {
//            ImageReplyDefault(fileName = "a file with a very very very - long name.png")
//        }
//    }
//}
//
//
//@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
//@Composable
//private fun ReplyToTextPillPreview() {
//    InitMessengerPreview {
//        ReplyToPill(PreviewReplyToViewModel()) {
//            TextReply(text = "a text that is very very very very looooooong", maxLines = 2)
//        }
//    }
//}
//
//@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
//@Composable
//private fun ReferencedPillPreview() {
//    InitMessengerPreview {
//        ReferencedMessagePill(senderName = "Martin", content = { Text(text = "Hi") })
//    }
//}
