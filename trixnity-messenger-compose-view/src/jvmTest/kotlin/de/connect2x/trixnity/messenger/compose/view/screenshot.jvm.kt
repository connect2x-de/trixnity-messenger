package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

actual suspend fun SemanticsNodeInteraction.screenshot(path: String) {
    val image = captureToImage()
    val bytes = image.encodeToByteArray(ImageFormat.PNG, quality = 1)
    Path(path).parent?.createDirectories()
    Path(path).writeBytes(bytes)
}
