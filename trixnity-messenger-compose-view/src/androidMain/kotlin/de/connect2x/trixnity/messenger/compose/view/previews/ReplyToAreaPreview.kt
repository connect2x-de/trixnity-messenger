package de.connect2x.trixnity.messenger.compose.view.previews

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.trixnity.messenger.compose.view.Platform
import de.connect2x.trixnity.messenger.compose.view.PlatformType
import de.connect2x.trixnity.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.compose.view.room.timeline.ReplyToArea
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.PreviewInputAreaViewModel

@Preview
@Composable
private fun ReplyToAreaPreview() {
    InitMessengerPreview {
        CompositionLocalProvider(Platform provides PlatformType.ANDROID) {
            Column {
                ReplyToArea(
                    inputAreaViewModel = PreviewInputAreaViewModel()
                )
            }
        }
    }
}
