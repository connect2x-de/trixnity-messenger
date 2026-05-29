package de.connect2x.trixnity.messenger.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.byteArrayFlowFromSource
import io.ktor.http.*
import okio.Buffer
import okio.source

private val log: Logger = Logger("de.connect2x.trixnity.messenger.util.UriFileDescriptorKt")

class UriFileDescriptor(private val context: Context, internal val fileUri: Uri, i18n: I18n) : FileDescriptor {

    private val computedFileInfo = getComputeFileInfo(fileUri)

    override val fileName: String = computedFileInfo?.fileName ?: i18n.commonUnknown()
    override val fileSize: Long? = computedFileInfo?.fileSize
    override val mimeType: ContentType? =
        computedFileInfo?.mimeType?.let(ContentType.Companion::parse)
            ?: computedFileInfo?.fileName?.let(ContentType.Companion::fromFilePath)?.firstOrNull()
    override val content: ByteArrayFlow = byteArrayFlowFromSource {
        log.debug { "File size is $fileSize" }
        context.contentResolver.openInputStream(fileUri)?.source() ?: Buffer()
    }

    private fun getComputeFileInfo(uri: Uri): ComputedFileInfo? =
        runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        return@use ComputedFileInfo(
                            cursor.getString(nameIndex),
                            cursor.getLong(sizeIndex),
                            context.contentResolver.getType(uri),
                        )
                    } else return@use null
                }
            }
            .getOrNull()
}

data class ComputedFileInfo(val fileName: String, val fileSize: Long?, val mimeType: String?)
