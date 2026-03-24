package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.waitUntilExactlyOneExists

@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.screenshot(testName: String, name: String) {
    val imageName = if (name.endsWith(".jpg")) name else "$name.jpg"

    waitUntilExactlyOneExists(hasTestTag("ClientSurface"), 3_000)
    onNodeWithTag("ClientSurface")
        .screenshot("screenshots/$platformName/$testName/$imageName")
}
