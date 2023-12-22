package de.connect2x.trixnity.messenger.util

import io.ktor.http.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.folivo.trixnity.utils.byteArrayFlowFromReadableStream
import org.koin.core.module.Module
import org.koin.dsl.module
import web.file.File


actual class FileDescriptor(val handle: File?)

actual class FileDescriptorSerializer : KSerializer<FileDescriptor> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FileDescriptorSerializer")

    override fun deserialize(decoder: Decoder): FileDescriptor = FileDescriptor(null)

    override fun serialize(encoder: Encoder, value: FileDescriptor) {
    }
}

actual fun platformGetFileInfoModule(): Module = module {
    single<GetFileInfo> {
        GetFileInfo { fileDescriptor ->
            val jsFile = fileDescriptor.handle
            if (jsFile != null)
                FileInfo(
                    fileName = jsFile.name,
                    fileSize = jsFile.size.toInt(),
                    mimeType = ContentType.fromFilePath(jsFile.name).firstOrNull(),
                    content = byteArrayFlowFromReadableStream { jsFile.stream() }
                )
            else null
        }
    }
}