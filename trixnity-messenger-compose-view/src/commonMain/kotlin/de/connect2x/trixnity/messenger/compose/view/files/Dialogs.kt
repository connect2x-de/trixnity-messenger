package de.connect2x.trixnity.messenger.compose.view.files

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.common.FilePickerType
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.client.media.PlatformMedia


@Composable
expect fun SaveFileDialog(
    fileName: String,
    mimeType: String?,
    error: String?,
    downloadFile: (suspend (PlatformMedia) -> Unit, () -> Unit) -> Unit,
    onCloseSaveFileDialog: () -> Unit,
)

@Composable
expect fun LoadFileDialog(
    availableTypes: List<FilePickerType>,
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
)

expect fun filterFilePickerOptionsByAvailability(
    vararg availablePickerTypes: FilePickerType,
): List<FilePickerType>

