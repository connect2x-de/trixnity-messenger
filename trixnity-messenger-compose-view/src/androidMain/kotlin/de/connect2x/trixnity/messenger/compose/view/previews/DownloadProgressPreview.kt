package de.connect2x.trixnity.messenger.compose.view.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.trixnity.messenger.compose.view.common.DownloadProgress
import de.connect2x.trixnity.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun DownloadProgressPreview() {
    InitMessengerPreview {
        Box(Modifier.background(Color.Gray)) {
            DownloadProgress(
                progressElement = FileTransferProgressElement(0.3f, "3.4 / 7.9 MB"),
                cancel = {},
                color = Color.DarkGray,
            )
        }
    }
}
