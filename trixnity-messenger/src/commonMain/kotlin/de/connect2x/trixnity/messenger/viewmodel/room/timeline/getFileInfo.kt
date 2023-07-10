package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import io.ktor.http.*
import okio.Source

data class FileInfo(val fileName: String, val fileSize: Long?, val mimeType: ContentType, val source: Source)

interface GetFileInfo {
    operator fun invoke(file: String): FileInfo {
        return getFileInfo(file)
    }
}

expect fun getFileInfo(file: String): FileInfo