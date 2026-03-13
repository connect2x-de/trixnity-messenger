package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.printToLog

@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.printComposeTree(testName: String, matcher: SemanticsMatcher = hasTestTag("ClientSurface")) {
    waitUntilExactlyOneExists(testName, matcher).printToLog("ComposeTree")
}
