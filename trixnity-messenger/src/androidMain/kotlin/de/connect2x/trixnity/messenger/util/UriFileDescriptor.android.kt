package de.connect2x.trixnity.messenger.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import de.connect2x.trixnity.messenger.i18n.I18n
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.byteArrayFlowFromSource
import okio.Buffer
import okio.source
import okio.use
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

private val log = KotlinLogging.logger { }

class UriFileDescriptor(
    private val context: Context,
    val fileUri: Uri,
    private val i18n: I18n,
) : FileDescriptor {

    private val computedFileInfo = getComputeFileInfo(fileUri)

    override val fileName: String = computedFileInfo?.fileName ?: i18n.commonUnknown()
    override val fileSize: Int? = computedFileInfo?.fileSize
    override val mimeType: ContentType? =
        ContentType.fromFilePath(computedFileInfo?.fileName ?: i18n.commonUnknown()).firstOrNull()
    override val content: ByteArrayFlow = if (mimeType?.contentType == "image") {
        byteArrayFlowFromSource {
            val input = context.contentResolver.openInputStream(fileUri)
            if (input != null) {
                val degrees =
                    context.contentResolver.query(
                        fileUri,
                        arrayOf(MediaStore.Images.ImageColumns.ORIENTATION),
                        null,
                        null
                    )
                        .use { cursor ->
                            if (cursor != null && cursor.count == 1) {
                                cursor.moveToFirst()
                                cursor.getInt(0)
                            } else 0
                        }

                if (degrees != 0) rotateImage(input, degrees).source()
                else input.source()
            } else Buffer()
        }
    } else byteArrayFlowFromSource { context.contentResolver.openInputStream(fileUri)?.source() ?: Buffer() }

    private fun getComputeFileInfo(uri: Uri): ComputedFileInfo? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                return@use ComputedFileInfo(cursor.getString(nameIndex), cursor.getLong(sizeIndex).toInt())
            } else {
                return@use null
            }
        }
    }.getOrNull()

    private fun rotateImage(inputStream: InputStream, degrees: Int): InputStream {
        //TODO Make rotation dependent on file size because of in Memory operation
        val bitmap = BitmapFactory.decodeStream(inputStream)
        log.debug { "Rotating file of class" }
        if (bitmap != null) {
            val rotationMatrix = Matrix()
            rotationMatrix.postRotate(degrees.toFloat())
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotationMatrix, true)
            val byteOutput = ByteArrayOutputStream()
            val mimeType = when (mimeType?.contentSubtype) {
                "PNG" -> Bitmap.CompressFormat.PNG
                "JPEG" -> Bitmap.CompressFormat.JPEG
                else -> Bitmap.CompressFormat.JPEG
            }
            rotatedBitmap.compress(mimeType, 100, byteOutput)
            return ByteArrayInputStream(byteOutput.toByteArray())
        } else return inputStream
    }
}

data class ComputedFileInfo(val fileName: String, val fileSize: Int?)
