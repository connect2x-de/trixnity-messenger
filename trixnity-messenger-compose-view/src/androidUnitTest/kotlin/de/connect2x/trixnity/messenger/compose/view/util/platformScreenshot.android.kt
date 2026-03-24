package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.test.SemanticsNodeInteraction

actual suspend fun SemanticsNodeInteraction.screenshot(path: String) {
    throw Exception("tests requiring a MatrixMultiMessenger should be run as instrumented tests")
}
