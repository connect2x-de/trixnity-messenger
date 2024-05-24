package de.connect2x.trixnity.messenger.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.byteArrayFlowFromSource
import okio.Buffer
import okio.source

class UriFileDescriptor(private val context: Context, val fileUri: Uri) : FileDescriptor {

    private val fileDataInfo = getFileNameAndSize(fileUri)
    override val fileName: String = fileDataInfo?.first ?: "Unknown"
    override val fileSize: Int? = fileDataInfo?.second
    override val mimeType: ContentType? = ContentType.fromFilePath(fileDataInfo?.first ?: "").firstOrNull()
    override val content: ByteArrayFlow =
        byteArrayFlowFromSource { context.contentResolver.openInputStream(fileUri)?.source() ?: Buffer() }

    private fun getFileNameAndSize(uri: Uri): Pair<String, Int?>? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                return@use Pair(cursor.getString(nameIndex), cursor.getLong(sizeIndex).toInt())
            } else {
                return@use null
            }
        }
    }.getOrNull()
}
