package de.connect2x.trixnity.messenger.export

import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.writeTo
import okio.FileSystem
import okio.buffer
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformFileBasedExportRoomSinkWriter(): Module = module {
    single<FileBasedExportRoomSinkWriterFactory> {
        object : FileBasedExportRoomSinkWriterFactory {
            override fun create(
                destination: Destination,
                fileName: String
            ): FileBasedExportRoomSinkWriter =
                OkioFileBasedExportRoomSinkWriter(destination, fileName, get())
        }
    }
}

class OkioFileBasedExportRoomSinkWriter(
    private val destination: Destination,
    fileName: String,
    private val fileSystem: FileSystem,
) : FileBasedExportRoomSinkWriter {
    private val filePath = destination.resolve(fileName)
    private val fileSink = fileSystem.appendingSink(filePath).buffer()
    private val mediaPath = destination.resolve("media")
    override suspend fun start() {
        fileSystem.createDirectory(destination)
        fileSystem.createDirectory(mediaPath)
    }

    override suspend fun addContent(content: String) {
        fileSink.writeUtf8(content)
    }

    override suspend fun addMedia(content: ByteArrayFlow, filename: String) {
        val mediaSink = fileSystem.sink(mediaPath.resolve(filename)).buffer()
        content.writeTo(mediaSink)
        mediaSink.close()
    }

    override suspend fun finish() {
        fileSink.close()
    }
}
