package de.connect2x.messenger.compose.view.files

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
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
import org.jetbrains.skia.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File


private val log = KotlinLogging.logger {}

actual fun imageBitmapFromBytes(encodedImageData: ByteArray): ImageBitmap? {
    return try {
        Image.makeFromEncoded(encodedImageData).toComposeImageBitmap()
    } catch (e: Exception) {
        log.error(e) { "cannot create imageBitmapFromBytes" }
        null
    }
}

private var oldPosition: Offset? = null

@OptIn(ExperimentalComposeUiApi::class)
actual fun mouseEventsForImageOverlay(
    maxWidth: Float,
    maxHeight: Float,
    maxBoundsImage: Offset,
    scale: MutableState<Float>,
    move: MutableState<Offset>,
    xMin: MutableState<Float>,
    yMin: MutableState<Float>,
): Modifier {
    val scaleFactorX = maxBoundsImage.x / maxWidth
    val scaleFactorY = maxBoundsImage.y / maxHeight
    return Modifier.mouseScrollFilter { event, _ ->
        val newScale = when (val delta = event.delta) {
            is MouseScrollUnit.Line -> scale.value - (0.1f * delta.value)
            is MouseScrollUnit.Page -> scale.value - (0.1f * delta.value)
        }
        scale.value = newScale.coerceIn(1f, 10f)
        val width = if (scaleFactorX * scale.value > 1) maxWidth else 0f
        val height = if (scaleFactorY * scale.value > 1) maxHeight else 0f
        xMin.value = width * (scale.value - 1) / 2
        yMin.value = height * (scale.value - 1) / 2

        true
    }.then(Modifier.pointerMoveFilter(onMove = { position: Offset ->
        if (oldPosition == null) {
            oldPosition = position
        }// to get the same amount of distance
        // when scaled, we have to move more pixels
        oldPosition?.let { // to get the same amount of distance
            // when scaled, we have to move more pixels
            val deltaX = it.x - position.x
            val deltaY = it.y - position.y
            val newMoveX =
                move.value.x + (deltaX * scaleFactorX.coerceAtMost(1f) * scale.value) // when scaled, we have to move more pixels
            val newMoveY =
                move.value.y + (deltaY * scaleFactorY.coerceAtMost(1f) * scale.value) // to get the same amount of distance
            move.value = Offset(
                newMoveX.coerceIn(-xMin.value, xMin.value),
                newMoveY.coerceIn(-yMin.value, yMin.value)
            )// to get the same amount of distance
            // when scaled, we have to move more pixels
            oldPosition = position
        }
        true
    }))
}

/**
 * [DesktopVideoPlayer](https://github.com/JetBrains/compose-jb/blob/master/components/VideoPlayer/library/src/desktopMain/kotlin/org/jetbrains/compose/videoplayer/DesktopVideoPlayer.kt)
 */
@Composable
actual fun VideoPlayer(width: Float, height: Float, url: String) {
}

@Composable
actual fun SaveDialog(
    fileName: String,
    mimeType: String?,
    error: String?,
    downloadFile: (suspend (ByteArrayFlow) -> Unit) -> Unit,
    onCloseSaveFileDialog: () -> Unit,
) {
    val i18n = DI.current.get<I18nView>()
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
actual fun LoadDialog(
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
    mode: LoadFileMode,
) {
    val i18n = DI.current.get<I18nView>()
    var isOpened by remember { mutableStateOf(false) }
    val fileSystem = DI.current.get<FileSystem>()
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

actual fun getClipboardFile(fileSystem: FileSystem): FileDescriptor? {
    log.debug { "access clipboard" }
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    return try {
        val files = clipboard.getData(DataFlavor.javaFileListFlavor) as List<*>
        if (files.isNotEmpty()) {
            files[0]?.let { file ->
                if (file is File) PathFileDescriptor(
                    file.absolutePath.toPath(),
                    fileSystem = fileSystem
                ) else null
            }
        } else {
            log.info { "the selected files list is empty" }
            null
        }
    } catch (exc: UnsupportedFlavorException) {
        log.info { "the content of the clipboard is no file" }
        null
    }
}
