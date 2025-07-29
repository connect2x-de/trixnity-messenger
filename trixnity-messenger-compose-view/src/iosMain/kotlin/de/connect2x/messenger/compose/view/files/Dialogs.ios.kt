package de.connect2x.messenger.compose.view.files

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.FilePickerType
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.util.FileDescriptor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write
import net.folivo.trixnity.client.media.PlatformMedia

private val log = KotlinLogging.logger {}

@Composable
actual fun SaveFileDialog(
    fileName: String,
    mimeType: String?,
    error: String?,
    downloadFile: (suspend (PlatformMedia) -> Unit) -> Unit,
    onCloseSaveFileDialog: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val hasError = error?.isNotBlank() == true
    if (hasError) {
        ThemedModalDialog(onCloseSaveFileDialog) {
            ModalDialogHeader {
                Text(i18n.fileDialogDownloadErrorSave())
            }
            ModalDialogContent {
                Text(error)
            }
            ModalDialogFooter {
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    onClick = onCloseSaveFileDialog,
                ) {
                    Text(i18n.actionOk())
                }
            }
        }
    }
    LaunchedEffect(hasError) {
        if (!hasError) downloadFile { file ->
            val savedFile = FileKit.openFileSaver(
                suggestedName = fileName.substringBeforeLast("."),
                extension = fileName.substringAfterLast("."),
                // TODO: set initialDirectory to OS dependent default pictures directory
            )
            try {
                savedFile?.let {
                    file.toByteArray()?.let { bytes ->
                        savedFile.write(bytes)
                    }
                } ?: log.warn { "No valid path selected" }
            } finally {
                onCloseSaveFileDialog()
            }
        }
    }
}

@Composable
actual fun LoadFileDialog(
    availableTypes: List<FilePickerType>,
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val launcher = rememberFilePickerLauncher(
        type = when {
            availableTypes.size == 1 && availableTypes.first() == FilePickerType.IMAGE_FILE -> FileKitType.Image
            availableTypes.size == 1 && availableTypes.first() == FilePickerType.IMAGE_AND_VIDEO_FILE -> FileKitType.ImageAndVideo
            else -> FileKitType.File()
        },
        mode = FileKitMode.Single,
        title = i18n.fileDialogTitleLoad()
    ) { file ->
        file?.let {
            // We don't use the default PathFileDescriptor here because requires some special behaviour and to prevent
            // a crash when selecting a file.
            onFileSelect(FileKitFileDescriptor(file))
        }
        onCloseLoadFileDialog()
    }
    LaunchedEffect(Unit) { // To be safe, wrap the `launch` call.
        launcher.launch()
    }
}

actual fun filterFilePickerOptionsByAvailability(
    vararg availablePickerTypes: FilePickerType,
): List<FilePickerType> = emptyList() // TODO
