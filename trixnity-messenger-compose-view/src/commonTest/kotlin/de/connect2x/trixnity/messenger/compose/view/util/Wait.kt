package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.waitUntilExactlyOneExists
import de.connect2x.trixnity.messenger.compose.view.messenger.screenshot
import kotlinx.coroutines.DelicateCoroutinesApi

@OptIn(ExperimentalTestApi::class, DelicateCoroutinesApi::class)
suspend fun ComposeUiTest.waitUntilExactlyOneExists(
    testName: String,
    matcher: SemanticsMatcher,
    timeoutMillis: Long = 5_000L,
): SemanticsNodeInteraction {
    try {
        waitUntilExactlyOneExists(matcher, timeoutMillis)
        return onNode(matcher)
    } catch (e: Throwable) {
        screenshot(testName, "Failure")
        onNodeWithTag("ClientSurface").printToLog("ComposeTree")
        throw e
    }
}
