package de.connect2x.trixnity.messenger.compose.view.files

import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.toByteArrayFlow
import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import kotlinx.io.files.Path

class ByteArrayFileDescriptor(private val path: Path, private val data: ByteArray) : FileDescriptor {
    override val fileName: String = path.name
    override val fileSize: Long = data.size.toLong()
    override val mimeType: ContentType? = ContentType.fromFilePath(fileName).firstOrNull()
    override val content: ByteArrayFlow = data.toByteArrayFlow()
}
