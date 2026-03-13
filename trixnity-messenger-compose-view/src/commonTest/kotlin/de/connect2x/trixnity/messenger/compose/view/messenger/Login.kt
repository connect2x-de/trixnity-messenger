package de.connect2x.trixnity.messenger.compose.view.messenger

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.test.waitUntilExactlyOneExists

@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.login(testName: String, username: String, password: String) {
    screenshot(testName, "Login - Before Server Discovery")
    onNodeWithText("Your Matrix Server", ignoreCase = true).assertExists().performTextInput("http://localhost:8008")

    waitUntilAtLeastOneExists(hasText("Login With Password", ignoreCase = true), timeoutMillis = 5_000L)
    onNodeWithText("Login With Password", ignoreCase = true).performClick()
    waitForIdle()
    screenshot(testName, "Login - Before Login")
    onNodeWithText("Your Matrix Username", ignoreCase = true).performTextInput(username)
    onNodeWithText("Your password", ignoreCase = true).performTextInput(password)
    waitForIdle()
    screenshot(testName, "Login - Wait for Login Button")
    onNodeWithText("Login", ignoreCase = true).performClick()
    waitForIdle()

    screenshot(testName, "Login - After Login")
    waitUntilExactlyOneExists(hasText("next"), timeoutMillis = 5_000L)
    onNodeWithText("next", ignoreCase = true).performClick()
    waitForIdle()

    waitUntilExactlyOneExists(hasText("create vault", ignoreCase = true), timeoutMillis = 5_000L)
    screenshot(testName, "Login - Before Create Vault")
    onNodeWithText("create vault", ignoreCase = true).performClick()
    waitForIdle()
}
