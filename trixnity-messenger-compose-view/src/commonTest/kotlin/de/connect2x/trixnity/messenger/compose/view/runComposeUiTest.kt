package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestResult

@OptIn(ExperimentalTestApi::class)
expect fun runComposeUiTest(block: suspend ComposeUiTestWithBackgroundScope.() -> Unit): TestResult

@OptIn(ExperimentalTestApi::class)
data class ComposeUiTestWithBackgroundScope(
    val composeUiTest: ComposeUiTest,
    val testScheduler: TestCoroutineScheduler,
    val backgroundScope: CoroutineScope,
)
