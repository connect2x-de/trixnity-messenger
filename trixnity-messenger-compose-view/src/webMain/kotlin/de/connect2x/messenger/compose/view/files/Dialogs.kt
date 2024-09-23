package de.connect2x.messenger.compose.view.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.JsFileDescriptor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.await
import net.folivo.trixnity.utils.ByteArrayFlow
import web.fs.FileSystemFileHandle
import kotlin.js.Promise


private val log = KotlinLogging.logger { }

@Composable
actual fun SaveDialog(
    fileName: String,
    mimeType: String?,
    error: String?,
    downloadFile: (suspend (ByteArrayFlow) -> Unit) -> Unit,
    onCloseSaveFileDialog: () -> Unit,
) {
    // FIXME
}

@Composable
actual fun LoadDialog(
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
    mode: LoadFileMode,
) {
    var disposed by remember { mutableStateOf(false) } // only upload file once
    LaunchedEffect(Unit) {
        if (disposed) return@LaunchedEffect
        disposed = true
        log.debug { "show file picker" }
        val jsCode = when (mode) {
            LoadFileMode.AnyFile -> js("window.showOpenFilePicker()")
            LoadFileMode.Picture -> js(
                """window.showOpenFilePicker(
                {
                    types: [
                        {
                            description: "Images",
                            accept: {
                                "image/*": [".png", ".gif", ".jpeg", ".jpg", ".bmp"],
                            }
                        }
                    ],
                    startIn: "pictures",
                    excludeAcceptAllOption: true,
                    multiple: false
                }
            )"""
            )
        }
        val fileHandles = (jsCode as Promise<Array<FileSystemFileHandle>>).await()
        log.debug { "pick file: $fileHandles" }
        if (fileHandles.isNotEmpty()) {
            val fileHandle = fileHandles[0]
            val file = fileHandle.getFile()
            onFileSelect(JsFileDescriptor(file = file))
        } else {
            log.warn { "nothing selected" }
        }
        onCloseLoadFileDialog() // TODO test this
    }
}
