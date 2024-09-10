package de.connect2x.messenger.previews

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.PlatformType
import de.connect2x.messenger.compose.view.room.timeline.ReplyToArea
import de.connect2x.messenger.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.PreviewReplyToViewModel2

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun ReplyToAreaPreview() {
    InitMessengerPreview {
        CompositionLocalProvider(Platform provides PlatformType.ANDROID) {
            Column {
                ReplyToArea(replyToViewModel = PreviewReplyToViewModel2())
            }
        }
    }
}
