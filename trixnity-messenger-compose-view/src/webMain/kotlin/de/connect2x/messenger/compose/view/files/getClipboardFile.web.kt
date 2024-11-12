package de.connect2x.messenger.compose.view.files

import de.connect2x.trixnity.messenger.util.FileDescriptor
import okio.FileSystem
import kotlin.Result

actual fun getClipboardFile(fileSystem: FileSystem): Result<FileDescriptor?> {
    // TODO https://developer.mozilla.org/en-US/docs/Web/API/Clipboard/read
    return Result.success(null)
}
