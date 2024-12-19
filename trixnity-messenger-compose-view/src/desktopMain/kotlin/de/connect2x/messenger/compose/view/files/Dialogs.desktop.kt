package de.connect2x.messenger.compose.view.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.FilePickerType
import de.connect2x.messenger.compose.view.common.FilePickerType.ATTACHMENT_FILE
import de.connect2x.messenger.compose.view.common.FilePickerType.IMAGE_AND_VIDEO_FILE
import de.connect2x.messenger.compose.view.common.FilePickerType.IMAGE_FILE
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.PathFileDescriptor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PickerType.Image
import io.github.vinceglb.filekit.core.PickerType.ImageAndVideo
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.write
import okio.FileSystem
import okio.Path.Companion.toPath


private val log = KotlinLogging.logger {}

@Composable
actual fun SaveFileDialog(
    fileName: String,
    mimeType: String?,
    error: String?,
    downloadFile: (suspend (ByteArrayFlow) -> Unit) -> Unit,
    onCloseSaveFileDialog: () -> Unit,
) {
    val hasError = error?.isNotBlank() == true
    if (hasError) DownloadErrorAlertDialog(error, onCloseSaveFileDialog)
    LaunchedEffect(hasError) {
        if (!hasError) downloadFile {
            val file = FileKit.saveFile(
                baseName = fileName.substringBeforeLast("."),
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
            availableTypes.size == 1 && availableTypes.first() == IMAGE_FILE -> Image
            availableTypes.size == 1 && availableTypes.first() == IMAGE_AND_VIDEO_FILE -> ImageAndVideo
            else -> PickerType.File()
        },
        mode = PickerMode.Single,
        title = i18n.fileDialogTitleLoad(),
        // TODO: set initialDirectory to OS dependent default pictures directory
    ) { file ->
        log.debug { "selected file: $file" }
        file?.let {
            file.path?.toPath()
                ?.let { onFileSelect(PathFileDescriptor(it, fileSystem)) }
                ?: run { log.error { "can't resolve path for selected file: $file" } }
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
