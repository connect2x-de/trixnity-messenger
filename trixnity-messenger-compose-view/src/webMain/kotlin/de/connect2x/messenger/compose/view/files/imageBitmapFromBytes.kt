package de.connect2x.messenger.compose.view.files

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun imageBitmapFromBytes(encodedImageData: ByteArray): ImageBitmap? {
    return Image.makeFromEncoded(encodedImageData).toComposeImageBitmap() // FIXME share this and more with desktop
}
