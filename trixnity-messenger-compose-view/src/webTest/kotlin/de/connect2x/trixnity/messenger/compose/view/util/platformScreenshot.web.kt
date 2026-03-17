package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.test.SemanticsNodeInteraction

actual suspend fun SemanticsNodeInteraction.screenshot(path: String) {
    // TODO no file system access, so we would need a HTTP server to send the screenshots to
}
