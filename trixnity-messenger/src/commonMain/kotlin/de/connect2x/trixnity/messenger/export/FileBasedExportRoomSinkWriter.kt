package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.messenger.util.FileDescriptor
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.utils.ByteArrayFlow
import org.koin.core.module.Module

interface FileBasedExportRoomSinkWriterFactory {
    fun create(
        roomId: RoomId,
        destination: FileDescriptor,
        fileName: String,
    ): FileBasedExportRoomSinkWriter
}

interface FileBasedExportRoomSinkWriter {
    suspend fun createFilesAndDirectories()
    suspend fun addContent(content: String)
    suspend fun addMedia(content: ByteArrayFlow, filename: String)
}

expect fun platformFileBasedExportRoomSinkWriter(): Module
