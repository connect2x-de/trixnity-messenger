package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.viewmodel.files.getFileSystem
import de.connect2x.trixnity.messenger.viewmodel.util.MimeTypes
import net.folivo.trixnity.utils.byteArrayFlow
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.source
import kotlin.io.path.inputStream

actual typealias FileDescriptor = String

actual suspend fun getFileInfo(fileDescriptor: FileDescriptor): FileInfo {
    val fileSystem: FileSystem = getFileSystem()
    val path = fileDescriptor.toPath()
    val fileName: String = path.name
    val fileSize: Long? = fileSystem.metadataOrNull(path)?.size
    val byteArrayFlow = byteArrayFlow(sourceFactory = {path.toNioPath().inputStream().source()})

    return FileInfo(fileName, fileSize, MimeTypes.guessByFileName(fileName), byteArrayFlow)
}