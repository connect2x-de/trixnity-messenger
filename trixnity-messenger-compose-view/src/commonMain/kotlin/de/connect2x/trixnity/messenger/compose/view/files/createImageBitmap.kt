package de.connect2x.trixnity.messenger.compose.view.files

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

expect inline fun createImageBitmap(width: Int, height: Int, crossinline drawPixel: (Int, Int) -> Color): ImageBitmap
