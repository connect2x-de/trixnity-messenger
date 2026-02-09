package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.utils.ByteArrayFlow
import org.koin.core.module.Module

interface FileBasedExportRoomSinkWriterFactory {
    fun create(
        destination: Destination,
        fileName: String,
    ): FileBasedExportRoomSinkWriter
}

interface FileBasedExportRoomSinkWriter {
    suspend fun start() {}
    suspend fun addContent(content: String)
    suspend fun addMedia(content: ByteArrayFlow, filename: String)
    suspend fun finish() {}
}

expect fun platformFileBasedExportRoomSinkWriter(): Module
