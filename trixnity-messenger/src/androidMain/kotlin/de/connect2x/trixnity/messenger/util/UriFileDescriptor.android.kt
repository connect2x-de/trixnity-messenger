package de.connect2x.trixnity.messenger.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import de.connect2x.trixnity.messenger.i18n.I18n
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.byteArrayFlowFromSource
import okio.Buffer
import okio.source
private val log = KotlinLogging.logger {  }
class UriFileDescriptor(
    private val context: Context,
    val fileUri: Uri,
    private val i18n: I18n
) : FileDescriptor {

    private val computedFileInfo = getComputeFileInfo(fileUri)

    override val fileName: String = computedFileInfo?.fileName ?: i18n.commonUnknown()
    override val fileSize: Long? = computedFileInfo?.fileSize
    override val mimeType: ContentType? =
        ContentType.fromFilePath(computedFileInfo?.fileName ?: i18n.commonUnknown()).firstOrNull()
    override val content: ByteArrayFlow =
        byteArrayFlowFromSource { log.debug { "File size is $fileSize" }
            context.contentResolver.openInputStream(fileUri)?.source() ?: Buffer() }

    private fun getComputeFileInfo(uri: Uri): ComputedFileInfo? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                return@use ComputedFileInfo(cursor.getString(nameIndex), cursor.getLong(sizeIndex))
            } else {
                return@use null
            }
        }
    }.getOrNull()
}

data class ComputedFileInfo(val fileName: String, val fileSize: Long?)
