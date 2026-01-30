package de.connect2x.trixnity.messenger.compose.view.files

import de.connect2x.trixnity.messenger.util.FileDescriptor
import okio.FileSystem

expect fun getClipboardFile(fileSystem: FileSystem, maxAttachmentSize: Long): Result<FileDescriptor?>

class NotPasteableException : IllegalArgumentException()
class EmptyFileListException : IllegalArgumentException()
