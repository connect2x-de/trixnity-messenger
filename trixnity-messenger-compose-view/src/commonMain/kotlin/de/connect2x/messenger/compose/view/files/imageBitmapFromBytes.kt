package de.connect2x.messenger.compose.view.files

import androidx.compose.ui.graphics.ImageBitmap

expect fun imageBitmapFromBytes(encodedImageData: ByteArray): ImageBitmap?
