package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.messenger.util.FileDescriptor
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.writeTo
import okio.FileSystem
import okio.buffer
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
                OkioFileBasedExportRoomSinkWriter(roomId, destination, fileName, get())
        }
    }
}

class OkioFileBasedExportRoomSinkWriter(
    roomId: RoomId,
    private val destination: FileDescriptor,
    fileName: String,
    private val fileSystem: FileSystem,
) : FileBasedExportRoomSinkWriter {
    private val filePath = destination.resolve(fileName)
    private val mediaPath = destination.resolve("media")
    override suspend fun createFilesAndDirectories() {
        fileSystem.createDirectory(destination)
        fileSystem.createDirectory(mediaPath)
    }

    override suspend fun addContent(content: String) {
        fileSystem.appendingSink(filePath).buffer().writeUtf8(content)
    }

    override suspend fun addMedia(content: ByteArrayFlow, filename: String) {
        content.writeTo(fileSystem.sink(mediaPath.resolve(filename)).buffer())
    }
}
