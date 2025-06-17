package de.connect2x.messenger.compose.view.files

import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.common.FilePickerType
import de.connect2x.trixnity.messenger.util.FileDescriptor
import net.folivo.trixnity.client.media.PlatformMedia

@Composable
actual fun SaveFileDialog(
    fileName: String,
    mimeType: String?,
    error: String?,
    downloadFile: (suspend (PlatformMedia) -> Unit) -> Unit,
    onCloseSaveFileDialog: () -> Unit,
) {
    // TODO
}

@Composable
actual fun LoadFileDialog(
    availableTypes: List<FilePickerType>,
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
) {
    // TODO
}

actual fun filterFilePickerOptionsByAvailability(
    vararg availablePickerTypes: FilePickerType,
): List<FilePickerType> = emptyList() // TODO
