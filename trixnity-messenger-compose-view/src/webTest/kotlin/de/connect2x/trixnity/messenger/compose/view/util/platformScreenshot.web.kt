package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage

actual suspend fun SemanticsNodeInteraction.screenshot(path: String) {
    captureToImage()
}
