package de.connect2x.messenger.compose.view.files

import de.connect2x.trixnity.messenger.util.FileDescriptor
import okio.FileSystem

expect fun getClipboardFile(fileSystem: FileSystem): FileDescriptor?
