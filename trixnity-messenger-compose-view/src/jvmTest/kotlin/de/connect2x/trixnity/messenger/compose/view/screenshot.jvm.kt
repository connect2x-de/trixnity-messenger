package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import java.io.File

actual suspend fun SemanticsNodeInteraction.screenshot(path: String) {
    val image = captureToImage()
    val bytes = image.encodeToByteArray(ImageFormat.PNG, quality = 1)
    File("screenshots/$path").writeBytes(bytes)
}
