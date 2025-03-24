package de.connect2x.messenger.compose.view.files

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.FilePickerType
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
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
    availableTypes: List<FilePickerType>,
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
)

expect fun filterFilePickerOptionsByAvailability(
    vararg availablePickerTypes: FilePickerType,
): List<FilePickerType>

@Composable
fun DownloadErrorAlertDialog(
    error: String,
    onCloseSaveFileDialog: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    AlertDialog(
        modifier = Modifier.defaultMinSize(minWidth = 400.dp),
        onDismissRequest = onCloseSaveFileDialog,
        title = { Text(i18n.fileDialogDownloadErrorSave()) },
        dismissButton = {
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                onClick = onCloseSaveFileDialog,
            ) {
                Text(i18n.commonOk())
            }
        },
        confirmButton = {},
        shape = RoundedCornerShape(8.dp),
        text = { Text(error) },
    )
}
