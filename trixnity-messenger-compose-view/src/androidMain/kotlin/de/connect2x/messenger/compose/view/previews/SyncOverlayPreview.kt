package de.connect2x.messenger.compose.view.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.messenger.compose.view.root.SyncOverlay
import de.connect2x.trixnity.messenger.viewmodel.initialsync.PreviewSyncViewModel

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun SyncOverlayPreview() {
    InitMessengerPreview {
        SyncOverlay(PreviewSyncViewModel())
    }
}
