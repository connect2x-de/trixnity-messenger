package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.OverflowingText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.OverflowingFileInfoDisplayMode.FILENAME_AND_INFO
import de.connect2x.messenger.compose.view.room.timeline.OverflowingFileInfoDisplayMode.FILENAME_ONLY
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.AudioMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.FileBasedMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ImageMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.VideoMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.formatDuration
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun OverflowingFileInfo(
    roomMessageViewModel: FileBasedMessageViewModel,
    displayMode: OverflowingFileInfoDisplayMode,
    modifier: Modifier,
    color: Color = Color.Unspecified,
) {
    val fileInfo = when (displayMode) {
        FILENAME_AND_INFO -> formatFileMetadata(roomMessageViewModel)
        FILENAME_ONLY -> roomMessageViewModel.fileName
    }
    val indexOfLastPeriod = roomMessageViewModel.fileName.lastIndexOf('.')
    val indexToCut = min(
        if (indexOfLastPeriod > -1) indexOfLastPeriod else roomMessageViewModel.fileName.length,
        fileInfo.length,
    )
    OverflowingText(
        fileInfo,
        indexToCut,
        TextOverflow.Ellipsis,
        modifier = modifier,
        color = color,
    )
}

enum class OverflowingFileInfoDisplayMode {
    FILENAME_ONLY, FILENAME_AND_INFO,
}


@Composable
fun formatFileMetadata(roomMessageViewModel: RoomMessageViewModel): String {
    val i18n = DI.get<I18nView>()
    return when (roomMessageViewModel) {

        is VideoMessageViewModel ->
            "${roomMessageViewModel.fileName}:" +
                    (roomMessageViewModel.duration?.let { " " + formatDuration(it.milliseconds) } ?: "") +
                    (roomMessageViewModel.fileSize?.let { " " + formatSize(it.toLong()) } ?: "")

        is AudioMessageViewModel ->
            "${roomMessageViewModel.fileName}:" +
                    (roomMessageViewModel.duration?.let { " " + formatDuration(it.milliseconds) } ?: "") +
                    (roomMessageViewModel.fileSize?.let { " " + formatSize(it.toLong()) } ?: "")

        is ImageMessageViewModel ->
            "${roomMessageViewModel.fileName}:" +
                    (roomMessageViewModel.fileSize?.let { " " + formatSize(it.toLong()) } ?: "")

        is FileBasedMessageViewModel ->
            "${roomMessageViewModel.fileName}:" +
                    (roomMessageViewModel.fileSize?.let { " " + formatSize(it.toLong()) } ?: "")

        else -> i18n.unknownFileInfo()
    }
}
