package de.connect2x.messenger.compose.view.files

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.util.FileDescriptor
import net.folivo.trixnity.utils.ByteArrayFlow


@Composable
expect fun SaveFileDialog(
    fileName: String,
    mimeType: String?,
    error: String?,
    downloadFile: (suspend (ByteArrayFlow) -> Unit) -> Unit,
    onCloseSaveFileDialog: () -> Unit,
)

@Composable
expect fun LoadFileDialog(
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
    mode: LoadFileMode,
)

enum class LoadFileMode {
    AnyFile,
    Picture,
}
