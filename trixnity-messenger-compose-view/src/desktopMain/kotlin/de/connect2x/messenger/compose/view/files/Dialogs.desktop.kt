package de.connect2x.messenger.compose.view.files

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.PathFileDescriptor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
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
    val i18n = DI.get<I18nView>()
    var isOpened by remember { mutableStateOf(false) }
    if (error != null) AlertDialog(
        modifier = Modifier.defaultMinSize(minWidth = 400.dp),
        onDismissRequest = {
            onCloseSaveFileDialog()
        },
        title = { Text(i18n.fileDialogDownloadErrorSave()) },
        dismissButton = {
            Button({
                onCloseSaveFileDialog()
            }, Modifier.buttonPointerModifier()) { Text(i18n.commonOk()) }
        },
        confirmButton = {},
        shape = RoundedCornerShape(8.dp),
        text = { Text(error) },
    )
    LaunchedEffect(Unit) {
        if (isOpened) return@LaunchedEffect
        isOpened = true
        downloadFile {
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
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
    mode: LoadFileMode,
) {
    val i18n = DI.get<I18nView>()
    var isOpened by remember { mutableStateOf(false) }
    val fileSystem = DI.get<FileSystem>()
    // Due to compose life cycles the launcher needs to be set up even if launch() is skipped.
    val launcher = rememberFilePickerLauncher(
        type = when (mode) {
            LoadFileMode.Picture -> PickerType.Image
            LoadFileMode.AnyFile -> PickerType.File()
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
            isOpened = false
            onCloseLoadFileDialog()
        }
    }
    LaunchedEffect(Unit) {
        if (isOpened) return@LaunchedEffect
        isOpened = true
        launcher.launch()
    }
}
