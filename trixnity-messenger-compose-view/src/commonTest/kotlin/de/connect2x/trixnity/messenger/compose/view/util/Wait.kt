package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.waitUntilExactlyOneExists
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalTestApi::class, DelicateCoroutinesApi::class)
suspend fun ComposeUiTest.waitUntilExactlyOneExists(
    testName: String,
    matcher: SemanticsMatcher,
    timeoutMillis: Long = 5_000L,
): SemanticsNodeInteraction {
    repeat(10) {
        delay(timeoutMillis / 10)
        withContext(Dispatchers.Default) { delay(timeoutMillis / 10) }

        try {
            waitUntilExactlyOneExists(matcher, timeoutMillis)
            return onNode(matcher)
        } catch (_: Throwable) { }
    }

    screenshot(testName, "Failure")
    onNodeWithTag("ClientSurface").printToLog("ComposeTree")

    throw ComposeTimeoutException(
        buildWaitUntilTimeoutMessage(matcher, timeoutMillis)
    )
}

private fun buildWaitUntilTimeoutMessage(
    matcher: SemanticsMatcher,
    timeoutMillis: Long,
): String = buildString {
    append("Condition ")
    append('(')
    append("exactly 1 nodes match (${matcher.description})")
    append(") ")
    append("still not satisfied after $timeoutMillis ms")
}
