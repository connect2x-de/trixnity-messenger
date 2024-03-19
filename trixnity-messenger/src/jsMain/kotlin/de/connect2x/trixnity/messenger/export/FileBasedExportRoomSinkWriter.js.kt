package de.connect2x.trixnity.messenger.export

import externals.jszip.ZipWriterStream
import js.promise.await
import js.typedarrays.Uint8Array
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.write
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformFileBasedExportRoomSinkWriter(): Module = module {
    single {
        object : FileBasedExportRoomSinkWriterFactory {
            override fun create(
                destination: Destination,
                fileName: String
            ): FileBasedExportRoomSinkWriter =
                WebZipFileBasedExportRoomSinkWriter(destination, fileName)
        }
    }
}

class WebZipFileBasedExportRoomSinkWriter(
    destination: Destination,
    fileName: String,
) : FileBasedExportRoomSinkWriter {
    private val zipper = ZipWriterStream()
    private val fileStream = zipper.writable<String>(fileName)
    private val fileStreamWriter = fileStream.getWriter()
    private val pipeToDestination = zipper.readable.pipeTo(destination.stream)

    override suspend fun addContent(content: String) {
        fileStreamWriter.write(content).await()
    }

    override suspend fun addMedia(content: ByteArrayFlow, filename: String) {
        val mediaStream = zipper.writable<Uint8Array>("media/$filename")
        mediaStream.write(content)
        mediaStream.close().await()
    }

    override suspend fun finish() {
        fileStream.close().await()
        zipper.close().await()
        pipeToDestination.await()
    }
}

