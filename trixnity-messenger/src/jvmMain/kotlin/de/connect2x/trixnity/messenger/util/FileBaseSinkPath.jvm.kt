package de.connect2x.trixnity.messenger.util

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer


actual fun fileBaseArchiveSink(fileName: String, resultContent: String) {
    // FiXMe: We need to add path for different OS window, linux.
    val path = System.getenv("HOME").toPath().resolve("Downloads").resolve(fileName)
    if (FileSystem.SYSTEM.exists(path)) {
        val sink = FileSystem.SYSTEM.appendingSink(path).buffer().apply {
            writeUtf8(resultContent)
            writeUtf8("\n")
        }
        sink.flush()
        sink.close()
    }else{
        FileSystem.SYSTEM.write(path) {
            writeUtf8(resultContent)
            writeUtf8("\n")
        }
    }
}
