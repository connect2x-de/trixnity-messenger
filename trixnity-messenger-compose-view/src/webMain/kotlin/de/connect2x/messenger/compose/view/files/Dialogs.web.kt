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
import de.connect2x.trixnity.messenger.util.JsFileDescriptor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PickerType.Image
import io.github.vinceglb.filekit.core.PickerType.ImageAndVideo
import net.folivo.trixnity.utils.ByteArrayFlow
import web.dom.document
import web.file.File
import web.fs.FileSystemFileHandle
import web.fs.FileSystemGetFileOptions
import web.html.HTML
import web.navigator.navigator
import web.timers.setTimeout
import web.url.URL
import web.window.WindowTarget
import kotlin.time.Duration.Companion.seconds


private val log = KotlinLogging.logger {}

/**
 * This component invokes a file picker by which the user can select
 * a file to be uploaded for where it may be needed or desired.
 * @param onFileSelect is invoked on successful selection of a file which
 * then provides a `FileDescriptor` with the byte stream.
 * @param onCloseLoadFileDialog is invoked on completion or conclusion
 * of the file picker.
 * @param mode indicates to the file picker what type of file or media
 * is being considered for file selection.
 */
@Composable
actual fun LoadFileDialog(
    availableTypes: List<FilePickerType>,
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    LaunchedEffect(Unit) {
        FileKit.pickFile(
            type = when {
                availableTypes.size == 1 && availableTypes.first() == IMAGE_FILE -> Image
                availableTypes.size == 1 && availableTypes.first() == IMAGE_AND_VIDEO_FILE -> ImageAndVideo
                else -> PickerType.File()
            },
            mode = PickerMode.Single,
            title = i18n.fileDialogTitleLoad(),
            initialDirectory = when {
                availableTypes.size == 1 && availableTypes.first() == IMAGE_FILE -> "pictures"
                else -> "downloads"
            },
        )?.let { file ->
            try {
                val descriptor = JsFileDescriptor(file.file as File)
                onFileSelect(descriptor)
            } catch (e: Throwable) {
                log.error(e) { "unable to upload file!" }
            }
            onCloseLoadFileDialog()
        } ?: let {
            log.error { "unable to resolve selected file!" }
            onCloseLoadFileDialog()
        }
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

/**
 * This component triggers a file download stream directly.
 * For this it requires a @param fileName, @param mimeType and
 * the byte stream via @param downloadFile.
 * If an error is provided via @param error, it will display an alert instead.
 * @param onCloseSaveFileDialog is invoked on completion.
 */
@Composable
actual fun SaveFileDialog(
    fileName: String,
    mimeType: String?,
    error: String?,
    downloadFile: (suspend (ByteArrayFlow) -> Unit) -> Unit,
    onCloseSaveFileDialog: () -> Unit,
) {
    val hasError = error?.isNotBlank() == true
    if (hasError) DownloadErrorAlertDialog(error ?: "", onCloseSaveFileDialog)
    LaunchedEffect(hasError) {
        if (!hasError) downloadFile {
            try {
                log.debug { "save file as fallback $fileName" }
                val fileHandle = navigator.storage.getDirectory()
                    .getFileHandle(
                        fileName,
                        FileSystemGetFileOptions.invoke(true),
                    )
                saveStreamToFile(fileName, fileHandle, it).onSuccess {
                    val file = fileHandle.getFile()
                    val fileUri = URL.createObjectURL(file)
                    log.debug { "completed saving: $fileName" }

                    // Invoke the download method that FileKit for web uses internally.
                    val a = document.createElement(HTML.a)
                    a.href = fileUri
                    a.download = fileName
                    a.target = WindowTarget._self
                    a.click() // Trigger the download.

                    setTimeout(15.seconds, {
                        URL.revokeObjectURL(fileUri)
                        log.debug { "file uri revoked for: $fileName" }
                    })
                }
            } catch (e: Exception) {
                // TODO: ignore abort error
                log.error(e) { "on catch" }
            }
            onCloseSaveFileDialog()
        }
    }
}

private suspend fun saveStreamToFile(
    fileName: String,
    fileHandle: FileSystemFileHandle,
    bytes: ByteArrayFlow,
): Result<Unit> {
    // TODO: use web worker where available
    log.debug { "begin download file stream: $fileName" }
    // TODO: resolve the writable as WritableStream<Uint8Array> instead to use existing write util
    val writable = fileHandle.createWritable()
    try {
        bytes.collect {
            log.debug { "write ${it.size} bytes for: $fileName" }
            writable.write(it)
        }
        log.debug { "completed download file stream: $fileName" }
        writable.close()
        return Result.success(Unit)

    } catch (e: Exception) {
        log.error(e) { "error downloading file stream: $fileName" }
        writable.close()
        return Result.failure(e)
    }
}
