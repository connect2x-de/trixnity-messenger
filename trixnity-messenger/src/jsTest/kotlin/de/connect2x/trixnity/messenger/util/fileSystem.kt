package de.connect2x.trixnity.messenger.util

import okio.FileSystem
import okio.NodeJsFileSystem

actual val fileSystem: FileSystem = NodeJsFileSystem
