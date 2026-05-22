package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import de.connect2x.lognity.test.TestBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope

@OptIn(ExperimentalTestApi::class, InternalComposeUiApi::class, InternalTestApi::class)
actual fun runComposeUiTest(block: suspend ComposeUiTestWithBackgroundScope.() -> Unit): TestResult {
    val testScheduler = StandardTestDispatcher()

    @Suppress("USELESS_CAST")
    return SkikoComposeUiTest(
            runTestContext = testScheduler,
            semanticsOwnerListener = null,
            coroutineDispatcher = testScheduler,
        )
        .runTest {
            val combined =
                ComposeUiTestWithBackgroundScope(
                    composeUiTest = this@runTest,
                    testScheduler = testScheduler.scheduler,
                    backgroundScope = CoroutineScope(currentCoroutineContext() + SupervisorJob()),
                )

            // Just used as a marker, typically we have a real TestScope but ComposeUiTest does not expose it
            TestBackend.testScope = TestScope()

            try {
                with(combined) { block() }
            } finally {
                combined.backgroundScope.cancel()
                TestBackend.testScope = null
            }
        } as TestResult
}
