package de.connect2x.messenger.compose.view.files

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
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
import de.connect2x.trixnity.messenger.util.JsFileDescriptor
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.client.media.indexeddb.IndexeddbPlatformMedia
import de.connect2x.trixnity.client.media.opfs.OpfsPlatformMedia
import web.dom.document
import web.file.File
import web.html.HtmlTagName
import web.timers.setTimeout
import web.url.URL
import web.window.WindowTarget
import web.window._self
import kotlin.time.Duration.Companion.seconds

private val log: Logger = Logger("de.connect2x.messenger.compose.view.files.DialogsKt")

/**
 * This component invokes a file picker by which the user can select
 * a file to be uploaded for where it may be needed or desired.
 * @param onFileSelect is invoked on successful selection of a file which
 * then provides a `FileDescriptor` with the byte stream.
 * @param onCloseLoadFileDialog is invoked on completion or conclusion
 * of the file picker.
 * @param availableTypes indicates to the file picker which types of files
 * or media are being considered for file selection.
 */
@Composable
actual fun LoadFileDialog(
    availableTypes: List<FilePickerType>,
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    LaunchedEffect(Unit) {
        FileKit.openFilePicker(
            type = when {
                availableTypes.size == 1 && availableTypes.first() == IMAGE_FILE -> FileKitType.Image
                availableTypes.size == 1 && availableTypes.first() == IMAGE_AND_VIDEO_FILE -> FileKitType.ImageAndVideo
                else -> FileKitType.File()
            },
            mode = FileKitMode.Single,
            title = i18n.fileDialogTitleLoad()
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
        if (!hasError) downloadFile({
            log.debug { "save file as fallback $fileName" }
            val file = when (it) {
                is OpfsPlatformMedia -> it.getTemporaryFile().getOrNull()?.file
                is IndexeddbPlatformMedia -> it.getTemporaryFile().getOrNull()?.file
                else -> null
            } ?: throw Exception("Unable to create temporary file")
            val fileUri = URL.createObjectURL(file)
            log.debug { "completed saving: $fileName" }

            // Invoke the download method that FileKit for web uses internally.
            val a = document.createElement(HtmlTagName.a)
            a.href = fileUri
            a.download = fileName
            a.target = WindowTarget._self
            a.click() // Trigger the download.

            setTimeout(15.seconds) {
                URL.revokeObjectURL(fileUri)
                log.debug { "file uri revoked for: $fileName" }
            }
            onCloseSaveFileDialog()
        }, onCloseSaveFileDialog)
    }
}
