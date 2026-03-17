package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithTag

suspend fun SemanticsNodeInteractionsProvider.screenshot(testName: String, name: String) {
    val imageName = if (name.endsWith(".png")) name else "$name.png"

    onNodeWithTag("ClientSurface")
        .screenshot("screenshots/$platformName/$testName/$imageName")
}
