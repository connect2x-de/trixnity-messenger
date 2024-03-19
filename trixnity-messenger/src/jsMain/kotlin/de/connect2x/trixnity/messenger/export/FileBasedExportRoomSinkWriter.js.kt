package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.messenger.util.FileDescriptor
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.utils.ByteArrayFlow
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformFileBasedExportRoomSinkWriter(): Module = module {
    single {
        object : FileBasedExportRoomSinkWriterFactory {
            override fun create(
                roomId: RoomId,
                destination: FileDescriptor,
                fileName: String
            ): FileBasedExportRoomSinkWriter =
                WebZipFileBasedExportRoomSinkWriter(roomId, destination, fileName)
        }
    }
}

// FIXME include https://github.com/gildas-lormeau/zip.js?
class WebZipFileBasedExportRoomSinkWriter(
    roomId: RoomId,
    destination: FileDescriptor,
    fileName: String,
) : FileBasedExportRoomSinkWriter {
    override suspend fun createFilesAndDirectories() {

    }

    override suspend fun addContent(content: String) {

    }

    override suspend fun addMedia(content: ByteArrayFlow, filename: String) {

    }
}

