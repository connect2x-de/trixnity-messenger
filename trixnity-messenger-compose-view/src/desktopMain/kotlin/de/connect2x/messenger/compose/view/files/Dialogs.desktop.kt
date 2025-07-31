package de.connect2x.messenger.compose.view.files

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.FilePickerType
import de.connect2x.messenger.compose.view.common.FilePickerType.ATTACHMENT_FILE
import de.connect2x.messenger.compose.view.common.FilePickerType.IMAGE_AND_VIDEO_FILE
import de.connect2x.messenger.compose.view.common.FilePickerType.IMAGE_FILE
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.PathFileDescriptor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.path
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.utils.write
import okio.FileSystem
import okio.Path.Companion.toPath


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
        if (!hasError) downloadFile {
            val file = FileKit.openFileSaver(
                suggestedName = fileName.substringBeforeLast("."),
                extension = fileName.substringAfterLast("."),
                // TODO: set initialDirectory to OS dependent default pictures directory
            )
            try {
                val path = file?.path?.toPath()
                if (path != null) FileSystem.SYSTEM.write(path, it)
                else log.warn { "no valid path selected" }
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
    val fileSystem = DI.get<FileSystem>()
    val launcher = rememberFilePickerLauncher(
        type = when {
            availableTypes.size == 1 && availableTypes.first() == IMAGE_FILE -> FileKitType.Image
            availableTypes.size == 1 && availableTypes.first() == IMAGE_AND_VIDEO_FILE -> FileKitType.ImageAndVideo
            else -> FileKitType.File()
        },
        mode = FileKitMode.Single,
        title = i18n.fileDialogTitleLoad(),
        // TODO: set initialDirectory to OS dependent default pictures directory
    ) { file ->
        log.debug { "selected file: $file" }
        file?.let {
            onFileSelect(PathFileDescriptor(file.path.toPath(), fileSystem))
        }
        onCloseLoadFileDialog()
    }
    LaunchedEffect(Unit) { // To be safe, wrap the `launch` call.
        launcher.launch()
    }
}

actual fun filterFilePickerOptionsByAvailability(
    vararg availablePickerTypes: FilePickerType,
): List<FilePickerType> {
    val supportedTypes = listOf(
        IMAGE_FILE, IMAGE_AND_VIDEO_FILE, ATTACHMENT_FILE,
    )
    return availablePickerTypes.filter { supportedTypes.contains(it) }
}
