package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import kotlin.io.encoding.Base64

actual suspend fun SemanticsNodeInteraction.screenshot(path: String) {
    val image = captureToImage()
    val bytes = image.encodeToByteArray(ImageFormat.JPEG, quality = 20)

    println("\n\n +++ $path\n${Base64.encode(bytes)}\n\n")
}
