package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import io.ktor.http.*
import js.promise.await
import js.typedarrays.Int8Array
import net.folivo.trixnity.utils.byteArrayFlow
import okio.Buffer
import web.filesystem.FileSystemFileHandle

actual class FileDescriptor(val handle: FileSystemFileHandle)

actual suspend fun getFileInfo(fileDescriptor: FileDescriptor): FileInfo {
    val jsFile = fileDescriptor.handle.getFile().await()
    // TODO File.stream() to avoid loading the file into memory
    val arrayBuffer = jsFile.arrayBuffer().await()
    return FileInfo(
        fileName = jsFile.name,
        fileSize = jsFile.size.toLong(),
        mimeType = ContentType.parse(jsFile.type),
        byteArrayFlow = byteArrayFlow { Buffer().write(Int8Array(arrayBuffer).unsafeCast<ByteArray>()) }
    )
}