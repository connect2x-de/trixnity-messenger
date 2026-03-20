package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.test.platform.app.InstrumentationRegistry
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.pathString

actual suspend fun SemanticsNodeInteraction.screenshot(path: String) {
    val image = captureToImage()
    val bytes = image.encodeToByteArray(ImageFormat.JPEG, quality = 20)

    val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    val dir = File(context.filesDir, Path(path).parent.pathString)
    dir.mkdirs()

    val file = File(context.filesDir, path)
    file.writeBytes(bytes)
}
