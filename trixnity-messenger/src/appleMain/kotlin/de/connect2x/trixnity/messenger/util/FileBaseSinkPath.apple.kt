package de.connect2x.trixnity.messenger.util

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import platform.Foundation.NSDownloadsDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask


actual fun fileBaseArchiveSink(fileName: String, resultContent: String) {
    val path = (NSSearchPathForDirectoriesInDomains(
        NSDownloadsDirectory,
        NSUserDomainMask,
        true
    )[0] as String) + "/$fileName"

    val localPath = path.toPath()
    if (FileSystem.SYSTEM.exists(localPath)) {
        val sink = FileSystem.SYSTEM.appendingSink(localPath).buffer().apply {
            writeUtf8(resultContent)
        }
        sink.flush()
        sink.close()
    } else {
        FileSystem.SYSTEM.write(localPath) {
            writeUtf8(resultContent)
        }
    }
}
