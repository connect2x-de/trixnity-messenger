package de.connect2x.trixnity.messenger.export

import externals.zipjs.BlobReader
import externals.zipjs.ZipWriter
import js.objects.jso
import js.typedarrays.toUint8Array
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.folivo.trixnity.utils.ByteArrayFlow
import org.koin.core.module.Module
import org.koin.dsl.module
import web.dom.document
import web.fs.FileSystemCreateWritableOptions
import web.fs.FileSystemDirectoryHandle
import web.fs.FileSystemFileHandle
import web.fs.FileSystemGetDirectoryOptions
import web.fs.FileSystemGetFileOptions
import web.fs.FileSystemRemoveOptions
import web.fs.FileSystemWritableFileStream
import web.html.HTML
import web.navigator.navigator
import web.timers.setTimeout
import web.url.URL
import web.window.WindowTarget
import kotlin.time.Duration.Companion.seconds


actual fun platformFileBasedExportRoomSinkWriter(): Module = module {
    single<FileBasedExportRoomSinkWriterFactory> {
        object : FileBasedExportRoomSinkWriterFactory {
            override fun create(
                destination: Destination,
                fileName: String
            ): FileBasedExportRoomSinkWriter = WebZipFileBasedExportRoomSinkWriter(fileName)
        }
    }
}

class WebZipFileBasedExportRoomSinkWriter(
    private val fileName: String,
) : FileBasedExportRoomSinkWriter {
    private val destination = fileName.substringBeforeLast('.') + ".zip"

    private lateinit var outputDirectory: FileSystemDirectoryHandle

    private lateinit var zipFile: FileSystemFileHandle
    private lateinit var zipStream: FileSystemWritableFileStream

    private lateinit var textFile: FileSystemFileHandle
    private lateinit var textStream: FileSystemWritableFileStream

    private lateinit var mediaFile: FileSystemFileHandle

    private lateinit var zipWriter: ZipWriter<dynamic>

    override suspend fun start() {
        outputDirectory = navigator.storage.getDirectory()
            .getDirectoryHandle("room-export", FileSystemGetDirectoryOptions(create = true))

        zipFile = outputDirectory.getFileHandle("archive.tmp", FileSystemGetFileOptions(create = true))
        zipStream = zipFile.createWritable(FileSystemCreateWritableOptions(keepExistingData = false))

        textFile = outputDirectory.getFileHandle("events.tmp", FileSystemGetFileOptions(create = true))
        textStream = textFile.createWritable(FileSystemCreateWritableOptions(keepExistingData = false))

        mediaFile = outputDirectory.getFileHandle("media.tmp", FileSystemGetFileOptions(create = true))

        zipWriter = ZipWriter(zipStream, jso {
            bufferedWrite = true
            dataDescriptor = true
            dataDescriptorSignature = true
        })

        super.start()
    }

    override suspend fun addContent(content: String) {
        textStream.write(content)
    }

    override suspend fun addMedia(content: ByteArrayFlow, filename: String) {
        val mediaStream = mediaFile.createWritable(FileSystemCreateWritableOptions(keepExistingData = false))

        content.collect {
            @OptIn(ExperimentalStdlibApi::class)
            mediaStream.write(it.toUint8Array())
        }

        mediaStream.close()

        zipWriter.add("media/${filename}", BlobReader(mediaFile.getFile())).await()
    }

    override suspend fun finish() {
        textStream.close()

        zipWriter.add(fileName, BlobReader(textFile.getFile())).await()
        zipWriter.close().await()

        val blobUrl = URL.createObjectURL(zipFile.getFile())

        val a = document.createElement(HTML.a)
        a.href = blobUrl
        a.download = destination
        a.target = WindowTarget._self
        a.click()

        setTimeout(60.seconds) {
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                URL.revokeObjectURL(blobUrl)
                navigator.storage.getDirectory().removeEntry(outputDirectory.name, FileSystemRemoveOptions(recursive = true))
            }
        }
    }
}

