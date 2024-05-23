package de.connect2x.trixnity.messenger.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArrayFlow

class FileDescriptorAndroid(
    val context: Context,
    private val fileUri: Uri,
) : FileDescriptor {


    private val fileDataInfo = getFileNameAndSize(fileUri)
    override val fileName: String
        get() = fileDataInfo?.first ?: "Unknown"
    override val fileSize: Int?
        get() = fileDataInfo?.second
    override val mimeType: ContentType?
        get() = ContentType.fromFilePath(fileDataInfo?.first ?: "").firstOrNull()
    override val content: ByteArrayFlow
        get() = toByteArrayFlow(fileUri)

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

    private fun toByteArrayFlow(uri: Uri): ByteArrayFlow =
        context.contentResolver.openInputStream(uri).use {
            val byteArray = it?.buffered()?.readBytes() ?: ByteArray(0)
            byteArray.toByteArrayFlow()
        }
}
