package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import io.ktor.http.*
import net.folivo.trixnity.utils.ByteArrayFlow

data class FileInfo(val fileName: String, val fileSize: Long?, val mimeType: ContentType, val byteArrayFlow: ByteArrayFlow)

interface GetFileInfo {
    suspend operator fun invoke(fileDescriptor: FileDescriptor): FileInfo {
        return getFileInfo(fileDescriptor)
    }
}

expect class FileDescriptor

expect suspend fun getFileInfo(fileDescriptor: FileDescriptor): FileInfo