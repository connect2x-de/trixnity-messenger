package de.connect2x.trixnity.messenger.util

import io.ktor.http.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.folivo.trixnity.utils.byteArrayFlowFromSource
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual typealias FileDescriptor = Path

actual class FileDescriptorSerializer : KSerializer<FileDescriptor> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FileDescriptorSerializer")

    override fun deserialize(decoder: Decoder): FileDescriptor =
        decoder.decodeString().toPath()

    override fun serialize(encoder: Encoder, value: FileDescriptor) {
        encoder.encodeString(value.toString())
    }
}

actual fun platformGetFileInfoModule(): Module = module {
    single<GetFileInfo> {
        val fileSystem = get<FileSystem>()
        GetFileInfo { fileDescriptor ->
            val fileName: String = fileDescriptor.name
            val fileSize: Long? = fileSystem.metadataOrNull(fileDescriptor)?.size
            val byteArrayFlow = byteArrayFlowFromSource { fileSystem.source(fileDescriptor) }

            FileInfo(fileName, fileSize?.toInt(), ContentType.fromFilePath(fileName).firstOrNull(), byteArrayFlow)
        }
    }
}