package de.connect2x.messenger.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.room.settings.TimelineElementMetadata
import de.connect2x.messenger.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PreviewTimelineElementMetadataViewModel1

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun TimelineElementMetadataPreview() {
    InitMessengerPreview {
        TimelineElementMetadata(
            viewModel = PreviewTimelineElementMetadataViewModel1(),
            isBottomOfStack = false,
            isSinglePane = true
        )
    }
}
