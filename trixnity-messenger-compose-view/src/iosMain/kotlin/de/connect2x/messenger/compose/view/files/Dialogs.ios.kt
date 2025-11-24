package de.connect2x.messenger.compose.view.files

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.FilePickerType
import de.connect2x.messenger.compose.view.common.FilePickerTypeSelection
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
import io.github.vinceglb.filekit.compressImage
import io.github.vinceglb.filekit.dialogs.FileKitCameraType
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.openCameraPicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.utils.toPath
import kotlinx.coroutines.flow.map
import kotlinx.io.Buffer
import net.folivo.trixnity.client.media.PlatformMedia

private val log = KotlinLogging.logger {}

@Composable
actual fun SaveFileDialog(
    fileName: String,
    mimeType: String?,
    error: String?,
    downloadFile: (suspend (PlatformMedia) -> Unit, () -> Unit) -> Unit,
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
        if (!hasError) downloadFile({ file ->
            val savedFile = FileKit.openFileSaver(
                suggestedName = fileName.substringBeforeLast("."),
                extension = fileName.substringAfterLast("."),
                // TODO: set initialDirectory to OS dependent default pictures directory
            )
            try {
                savedFile?.let {
                    // We couldn't use the ByteArrayFlowOkioExtensions from Trixnity because the API is exposing
                    // kotlinx.io's API. We create a buffer because kotlinx.io doesn't expose a function allowing
                    // to write a bytearray directly.
                    savedFile.sink().use { sink ->
                        file.map { data -> Buffer().also { it.write(data) } }.collect { data ->
                            data.use { buffer ->
                                sink.write(buffer, buffer.size)
                            }
                        }
                    }
                } ?: log.warn { "No valid path selected" }
            } finally {
                onCloseSaveFileDialog()
            }
        }, onCloseSaveFileDialog)
    }
}

@Composable
actual fun LoadFileDialog(
    availableTypes: List<FilePickerType>,
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
) {
    val selectedPickerType = remember { mutableStateOf<FilePickerType?>(null) }

    if (availableTypes.size == 1) {
        LaunchedEffect(availableTypes) {
            selectedPickerType.value = availableTypes.first()
        }
    } else {
        FilePickerTypeSelection(availableTypes, { selectedPickerType.value = it }, onCloseLoadFileDialog)
    }

    when (selectedPickerType.value) {
        FilePickerType.IMAGE_FILE, FilePickerType.IMAGE_AND_VIDEO_FILE, FilePickerType.ATTACHMENT_FILE -> {
            val i18n = DI.get<I18nView>()
            val launcher = rememberFilePickerLauncher(
                type = when (selectedPickerType.value) {
                    FilePickerType.IMAGE_FILE -> FileKitType.Image
                    FilePickerType.IMAGE_AND_VIDEO_FILE -> FileKitType.ImageAndVideo
                    else -> FileKitType.File()
                },
                mode = FileKitMode.Single,
                title = i18n.fileDialogTitleLoad(),
                onResult = {
                    it?.let {
                        onFileSelect(FileKitFileDescriptor(it))
                    }
                    onCloseLoadFileDialog()
                }
            )

            LaunchedEffect(Unit) {
                launcher.launch()
            }
        }

        FilePickerType.PHOTO_CAPTURE, FilePickerType.VIDEO_CAPTURE -> {
            LaunchedEffect(Unit) {
                // TODO: Use video camera picker when FileKit supports it
                val file = FileKit.openCameraPicker(type = FileKitCameraType.Photo)
                file?.let {
                    val compressed = FileKit.compressImage(file = file, quality = 70)
                    onFileSelect(ByteArrayFileDescriptor(it.path.toPath(), compressed))
                }
                onCloseLoadFileDialog()
            }
        }

        null -> {
            log.debug { "No file picker selected, don't show anything" }
        }
    }
}

actual fun filterFilePickerOptionsByAvailability(
    vararg availablePickerTypes: FilePickerType,
): List<FilePickerType> {
    val whitelist = listOf(
        FilePickerType.IMAGE_FILE,
        FilePickerType.IMAGE_AND_VIDEO_FILE,
        FilePickerType.ATTACHMENT_FILE,
        FilePickerType.PHOTO_CAPTURE
    )
    return availablePickerTypes.filter { whitelist.contains(it) }
}
