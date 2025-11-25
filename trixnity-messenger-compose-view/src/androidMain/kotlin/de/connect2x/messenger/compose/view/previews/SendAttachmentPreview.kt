package de.connect2x.messenger.compose.view.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.messenger.compose.view.room.timeline.SendAttachment
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.PreviewSendAttachmentViewModel

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun RoomSettingsPreview() {
    InitMessengerPreview {
        val sendAttachmentViewModel = PreviewSendAttachmentViewModel()
        sendAttachmentViewModel.error.value = "Anhang ist zu groß."
        SendAttachment(sendAttachmentViewModel)
    }
}
