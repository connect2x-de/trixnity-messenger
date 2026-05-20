@file:OptIn(ExperimentalWasmJsInterop::class)

package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.utils.ByteArrayFlow
import js.objects.unsafeJso
import js.typedarrays.toInt8Array
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.toJsString
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.module.Module
import org.koin.dsl.module
import web.dom.document
import web.fs.FileSystemDirectoryHandle
import web.fs.FileSystemFileHandle
import web.fs.FileSystemWritableFileStream
import web.fs.createWritable
import web.fs.getDirectoryHandle
import web.fs.getFile
import web.fs.getFileHandle
import web.fs.removeEntry
import web.fs.write
import web.html.HtmlTagName
import web.navigator.navigator
import web.storage.getDirectory
import web.streams.close
import web.timers.setTimeout
import web.url.URL
import web.window.WindowTarget
import web.window._self
import zipjs.BlobReader
import zipjs.ZipWriter

actual fun platformFileBasedExportRoomSinkWriter(): Module = module {
    single<FileBasedExportRoomSinkWriterFactory> {
        object : FileBasedExportRoomSinkWriterFactory {
            override fun create(destination: Destination, fileName: String): FileBasedExportRoomSinkWriter =
                WebZipFileBasedExportRoomSinkWriter(fileName)
        }
    }
}

class WebZipFileBasedExportRoomSinkWriter(private val fileName: String) : FileBasedExportRoomSinkWriter {
    private val destination = fileName.substringBeforeLast('.') + ".zip"

    private lateinit var outputDirectory: FileSystemDirectoryHandle

    private lateinit var zipFile: FileSystemFileHandle
    private lateinit var zipStream: FileSystemWritableFileStream

    private lateinit var textFile: FileSystemFileHandle
    private lateinit var textStream: FileSystemWritableFileStream

    private lateinit var mediaFile: FileSystemFileHandle

    private lateinit var zipWriter: ZipWriter<JsAny>

    override suspend fun start() {
        outputDirectory =
            navigator.storage.getDirectory().getDirectoryHandle("room-export", unsafeJso { create = true })

        zipFile = outputDirectory.getFileHandle("archive.tmp", unsafeJso { create = true })
        zipStream = zipFile.createWritable(unsafeJso { keepExistingData = false })

        textFile = outputDirectory.getFileHandle("events.tmp", unsafeJso { create = true })
        textStream = textFile.createWritable(unsafeJso { keepExistingData = false })

        mediaFile = outputDirectory.getFileHandle("media.tmp", unsafeJso { create = true })

        zipWriter =
            ZipWriter(
                zipStream,
                unsafeJso {
                    bufferedWrite = true
                    dataDescriptor = true
                    dataDescriptorSignature = true
                },
            )

        super.start()
    }

    override suspend fun addContent(content: String) {
        textStream.write(content.toJsString())
    }

    override suspend fun addMedia(content: ByteArrayFlow, filename: String) {
        val mediaStream = mediaFile.createWritable(unsafeJso { keepExistingData = false })

        content.collect { @OptIn(ExperimentalStdlibApi::class) mediaStream.write(it.toInt8Array()) }

        mediaStream.close()

        zipWriter.add("media/${filename}", BlobReader(mediaFile.getFile()))
    }

    override suspend fun finish() {
        textStream.close()

        zipWriter.add(fileName, BlobReader(textFile.getFile()))
        zipWriter.close()

        val blobUrl = URL.createObjectURL(zipFile.getFile())

        val a = document.createElement(HtmlTagName.a)
        a.href = blobUrl
        a.download = destination
        a.target = WindowTarget._self
        a.click()

        setTimeout(60.seconds) {
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                URL.revokeObjectURL(blobUrl)
                navigator.storage.getDirectory().removeEntry(outputDirectory.name, unsafeJso { recursive = true })
            }
        }
    }
}
