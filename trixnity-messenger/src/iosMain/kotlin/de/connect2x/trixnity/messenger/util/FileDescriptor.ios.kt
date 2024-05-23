package de.connect2x.trixnity.messenger.util

import io.ktor.http.ContentType
import net.folivo.trixnity.utils.ByteArrayFlow

class FileDescriptorIos(
    private val selectedFileName: String,
    private val selectedFileSize: Int?,
    private val selectedFileMimeType: ContentType?,
    private val selectedContentByteArrayFlow: ByteArrayFlow
) : FileDescriptor {
    override val fileName: String
        get() = selectedFileName
    override val fileSize: Int?
        get() = selectedFileSize
    override val mimeType: ContentType?
        get() = selectedFileMimeType
    override val content: ByteArrayFlow
        get() = selectedContentByteArrayFlow

}
