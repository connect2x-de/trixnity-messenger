package de.connect2x.trixnity.messenger.util

import io.ktor.http.ContentType
import net.folivo.trixnity.utils.ByteArrayFlow

class ManualFileDescriptor(
    override val fileName: String,
    override val fileSize: Int?,
    override val mimeType: ContentType?,
    override val content: ByteArrayFlow
) : FileDescriptor {}
