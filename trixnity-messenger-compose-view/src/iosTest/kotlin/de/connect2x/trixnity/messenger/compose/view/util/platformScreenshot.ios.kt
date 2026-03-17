package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import de.connect2x.trixnity.messenger.util.toNSData
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToURL

@OptIn(ExperimentalForeignApi::class)
actual suspend fun SemanticsNodeInteraction.screenshot(path: String) {
    val image = captureToImage()
    val bytes = image.encodeToByteArray(ImageFormat.PNG, quality = 1)

    val fileManager = NSFileManager.defaultManager
    val documentsUrl = fileManager.URLsForDirectory(
        directory = NSDocumentDirectory,
        inDomains = NSUserDomainMask,
    ).first() as NSURL
    val fileUrl = documentsUrl.URLByAppendingPathComponent(path)

    // Create parent directories
    val directoryUrl = fileUrl!!.URLByDeletingLastPathComponent()
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()

        fileManager.createDirectoryAtURL(
            url = directoryUrl!!,
            withIntermediateDirectories = true,
            attributes = null,
            error = error.ptr,
        )

        if (error.value != null) {
            error("Directory creation failed: ${error.value}")
        }
    }

    val nsData = bytes.toNSData()
    nsData.writeToURL(fileUrl, true)
}
