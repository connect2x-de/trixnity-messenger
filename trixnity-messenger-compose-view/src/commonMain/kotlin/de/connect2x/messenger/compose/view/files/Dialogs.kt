package de.connect2x.messenger.compose.view.files

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.files.PdfDocumentViewModel
import net.folivo.trixnity.utils.ByteArrayFlow


@Composable
expect fun SaveDialog(
    fileName: String,
    mimeType: String?,
    error: String?,
    downloadFile: (suspend (ByteArrayFlow) -> Unit) -> Unit,
    onCloseSaveFileDialog: () -> Unit,
)

@Composable
expect fun LoadDialog(
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
    mode: LoadFileMode,
)

@Composable
expect fun PDFReader(documentViewModel: PdfDocumentViewModel, scale: Float = 1f)

enum class LoadFileMode {
    AnyFile,
    Picture,
}
