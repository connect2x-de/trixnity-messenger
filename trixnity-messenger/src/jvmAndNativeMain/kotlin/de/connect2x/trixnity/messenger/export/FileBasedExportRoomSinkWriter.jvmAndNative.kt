package de.connect2x.trixnity.messenger.export

import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.writeTo
import okio.BufferedSink
import okio.FileSystem
import okio.buffer
import okio.use
import org.koin.core.module.Module
import org.koin.dsl.module


actual fun platformFileBasedExportRoomSinkWriter(): Module = module {
    single<FileBasedExportRoomSinkWriterFactory> {
        object : FileBasedExportRoomSinkWriterFactory {
            override fun create(
                destination: Destination,
                fileName: String,
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
    private lateinit var fileSink: BufferedSink
    private val mediaPath = destination.resolve("media")
    override suspend fun start() {
        fileSystem.createDirectory(destination, mustCreate = false)
        fileSystem.createDirectory(mediaPath, mustCreate = false)
        fileSink = fileSystem.appendingSink(filePath).buffer()
    }

    override suspend fun addContent(content: String) {
        fileSink.writeUtf8(content)
    }

    override suspend fun addMedia(content: ByteArrayFlow, filename: String) {
        fileSystem.sink(mediaPath.resolve(filename)).buffer().use {
            content.writeTo(it)
        }
    }

    override suspend fun finish() {
        fileSink.close()
    }
}
