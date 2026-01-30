package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.details

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.util.toByteArray
import de.connect2x.trixnity.messenger.util.toNSUrl
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.client.media.okio.OkioPlatformMedia
import org.jetbrains.compose.resources.decodeToImageBitmap
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.PDFKit.PDFDocument
import platform.PDFKit.kPDFDisplayBoxMediaBox
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContext
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

actual suspend fun getPlatformPDFReader(
    media: PlatformMedia,
    onError: (String?) -> Unit
): PDFReader = PlatformPDFReader(media, onError).also { it.initialize() }

private val log: Logger =
    Logger("de.connect2x.trixnity.messenger.compose.view.room.timeline.element.details.PdfTimelineElementDetailsViewKt")

fun UIImage.toByteArray(compressionQuality: Double = 0.9): ByteArray? =
    UIImageJPEGRepresentation(this, compressionQuality)?.toByteArray()

@OptIn(ExperimentalForeignApi::class)
class PlatformPDFReader(private val media: PlatformMedia, private val onError: (String?) -> Unit) : PDFReader {
    override val numOfPages: MutableState<Int?> = mutableStateOf(null)
    override val documentWidth: MutableState<Int?> = mutableStateOf(null)
    private val document: MutableState<PDFDocument?> = mutableStateOf(null)
    private val temporaryFile: MutableState<OkioPlatformMedia.TemporaryFile?> = mutableStateOf(null)

    suspend fun initialize() {
        log.debug { "Loading PDF..." }
        val fileResult = (media as OkioPlatformMedia).getTemporaryFile()
        if (fileResult.isFailure) {
            val errorMessage = fileResult.exceptionOrNull()?.message
            log.error { "cannot load pdf file into temporary file: $errorMessage" }
            onError(errorMessage)
            return
        }

        val pdfFile = requireNotNull(fileResult.getOrNull())
        val pdfDocument = try {
            PDFDocument(pdfFile.path.toNSUrl())
        } catch (_: NullPointerException) {
            log.error { "Unable to open PDF document: Unable to open PDF file" }
            onError("Unable to open PDF document")
            return
        }

        val firstPage = pdfDocument.pageAtIndex(0u)
        if (firstPage == null) {
            log.error { "Unable to open PDF document: Unable to acquire first page" }
            onError("Unable to load first page of PDF document")
            return
        }

        document.value = pdfDocument
        numOfPages.value = pdfDocument.pageCount.toInt()
        documentWidth.value = firstPage.boundsForBox(kPDFDisplayBoxMediaBox).useContents { size.width.toInt() }
        temporaryFile.value = pdfFile
    }

    override suspend fun getPage(pageId: Int, dpi: Float): ImageBitmap? {
        val page = document.value?.pageAtIndex(pageId.toULong()) ?: return null
        val pageBounds = page.boundsForBox(kPDFDisplayBoxMediaBox)
        val (width, height) = pageBounds.useContents { size.width to size.height }
        val scaleFactor = dpi / 72.0
        val (scaledWidth, scaledHeight) = Pair(width * scaleFactor, height * scaleFactor)

        // Render the PDF as image
        UIGraphicsBeginImageContext(CGSizeMake(scaledWidth, scaledHeight))
        val context = UIGraphicsGetCurrentContext() ?: return null

        // Background
        UIColor.whiteColor.setFill()
        CGContextFillRect(context, CGRectMake(0.0, 0.0, scaledWidth, scaledHeight))

        // PDF page
        CGContextSaveGState(context)
        CGContextTranslateCTM(context, 0.0, scaledHeight)
        CGContextScaleCTM(context, scaleFactor, -scaleFactor)
        page.drawWithBox(kPDFDisplayBoxMediaBox, context)
        CGContextRestoreGState(context)

        val data = UIGraphicsGetImageFromCurrentImageContext()?.toByteArray() ?: return null
        UIGraphicsEndImageContext()
        return data.decodeToImageBitmap()

    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onDispose() {
        GlobalScope.launch { temporaryFile.value?.delete() }
        document.value = null
    }
}
