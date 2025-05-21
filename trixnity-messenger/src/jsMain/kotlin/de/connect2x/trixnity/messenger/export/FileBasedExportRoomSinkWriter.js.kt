package de.connect2x.trixnity.messenger.export

import externals.jszip.ZipWriterStream
import js.buffer.ArrayBuffer
import js.typedarrays.Uint8Array
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.write
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformFileBasedExportRoomSinkWriter(): Module = module {
    single<FileBasedExportRoomSinkWriterFactory> {
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

    @OptIn(DelicateCoroutinesApi::class)
    private val pipeToDestination = GlobalScope.async {
        zipper.readable.pipeTo(destination.stream)
    }

    override suspend fun start() {
        super.start()
    }

    override suspend fun addContent(content: String) {
        fileStreamWriter.write(content)
    }

    override suspend fun addMedia(content: ByteArrayFlow, filename: String) {
        val mediaStream = zipper.writable<Uint8Array<ArrayBuffer>>("media/$filename")
        mediaStream.write(content)
        mediaStream.close()
    }

    override suspend fun finish() {
        fileStream.close()
        zipper.close().await()
        pipeToDestination.await()
    }
}

