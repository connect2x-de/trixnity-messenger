package de.connect2x.messenger.compose.view.files

import de.connect2x.trixnity.messenger.util.FileDescriptor
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.utils.toPath
import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import kotlinx.coroutines.flow.flow
import kotlinx.io.files.Path
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArrayFlow

class FileKitFileDescriptor(private val file: PlatformFile) : FileDescriptor {
    override val fileName: String = file.path.toPath().name
    override val fileSize: Long? = file.size()
    override val mimeType: ContentType? = ContentType.fromFilePath(fileName).firstOrNull()
    override val content: ByteArrayFlow = flow { emit(file.readBytes()) }
}

class ByteArrayFileDescriptor(private val path: Path, private val data: ByteArray) : FileDescriptor {
    override val fileName: String = path.name
    override val fileSize: Long? = data.size.toLong()
    override val mimeType: ContentType? = ContentType.fromFilePath(fileName).firstOrNull()
    override val content: ByteArrayFlow = data.toByteArrayFlow()
}
