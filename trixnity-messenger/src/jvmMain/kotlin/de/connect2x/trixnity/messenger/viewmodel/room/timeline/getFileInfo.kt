package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.viewmodel.files.getFileSystem
import de.connect2x.trixnity.messenger.viewmodel.util.MimeTypes
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.source
import kotlin.io.path.inputStream

actual fun getFileInfo(file: String): FileInfo {
    val fileSystem: FileSystem = getFileSystem()
    val path = file.toPath()
    val fileName: String = path.name
    val fileSize: Long? = fileSystem.metadataOrNull(path)?.size

    return FileInfo(fileName, fileSize, MimeTypes.guessByFileName(fileName), path.toNioPath().inputStream().source())
}