package de.connect2x.trixnity.messenger.compose.view.files

import de.connect2x.trixnity.messenger.util.FileDescriptor
import okio.FileSystem

actual fun getClipboardFile(fileSystem: FileSystem, maxAttachmentSize: Long): Result<FileDescriptor?> =
    Result.success(null) // TODO
