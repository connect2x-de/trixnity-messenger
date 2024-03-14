package de.connect2x.trixnity.messenger.util

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer


actual fun fileBaseArchiveSink(fileName: String, resultContent: String) {
    val downloadPath = when (getOs()) {
        OS.MAC_OS -> {
            System.getenv("HOME").toPath().resolve("Downloads")
        }
        OS.WINDOWS -> {
            System.getenv("USERPROFILE").toPath()
                .resolve("Downloads")
        }
        OS.LINUX -> {
            System.getenv("HOME") .toPath()
                .resolve("Downloads")
        }
    }

    val path = downloadPath.resolve(fileName)
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
