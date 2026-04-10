package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.waitUntilExactlyOneExists

/**
 * Take a screenshot of the given [surface] and save it under the given [name].
 *
 * @param testName used for the location of the screenshot for better traceability
 * @param name of the screenshot, if it does not end with .jpg, .jpg will be appended
 * @param surface if you have a different root than "ClientSurface" provide it here (Popups, etc.)
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.screenshot(testName: String, name: String, surface: String = "ClientSurface") {
    val imageName = if (name.endsWith(".jpg")) name else "$name.jpg"

    waitUntilExactlyOneExists(hasTestTag(surface), 3_000)
    onNodeWithTag(surface)
        .screenshot("screenshots/$platformName/$testName/$imageName")
}
