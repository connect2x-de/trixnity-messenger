@file:OptIn(ExperimentalTestApi::class)

package de.connect2x.trixnity.messenger.compose.view.messenger

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import de.connect2x.trixnity.messenger.compose.view.util.screenshot
import de.connect2x.trixnity.messenger.compose.view.util.waitUntilExactlyOneExists

suspend fun ComposeUiTest.login(testName: String, username: String, password: String) {
    serverLogin(testName, username, password)
    vault(testName)
    accountSetup(testName)
    selfVerification(testName)

    waitUntilExactlyOneExists(
        testName, hasText("confirm", ignoreCase = true)
                and hasAnyAncestor(hasTestTag("AccountSetupWizard"))
    )
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - AccountSetup - After Step 5")
}

private suspend fun ComposeUiTest.serverLogin(
    testName: String,
    username: String,
    password: String,
) {
    screenshot(testName, "Login - Login - Before Server Discovery")
    onNodeWithText("Your Matrix Server", ignoreCase = true).assertExists()
        .performTextInput("http://localhost:8008")

    waitUntilExactlyOneExists(testName, hasText("Login With Password", ignoreCase = true))
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - Login - Username+Password")
    onNodeWithText("Your Matrix Username", ignoreCase = true)
        .performTextInput(username)
    onNodeWithText("Your password", ignoreCase = true)
        .performTextInput(password)
    waitForIdle()
    onNodeWithText("Login", ignoreCase = true)
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - Login - After Login")
}

private suspend fun ComposeUiTest.vault(testName: String) {
    waitUntilExactlyOneExists(testName, hasText("create vault", ignoreCase = true))
    screenshot(testName, "Login - Vault - Create")
    onNodeWithText("create vault", ignoreCase = true).performClick()
    waitForIdle()

    waitUntilExactlyOneExists(testName, hasText("I have copied the recovery key", substring = true, ignoreCase = true))
    screenshot(testName, "Login - Vault - Created")

    onNodeWithText("I have copied the recovery key", substring = true, ignoreCase = true)
        .performClick()
    waitUntilExactlyOneExists(
        testName, hasText("next", ignoreCase = true)
                and isEnabled()
                and hasAnyAncestor(hasTestTag("CrossSigningBootstrapWizard"))
    )
        .performClick()

    screenshot(testName, "Login - Vault - Finished")
    waitUntilExactlyOneExists(testName, hasText("confirm", ignoreCase = true))
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - Vault - Confirmed")
}

private suspend fun ComposeUiTest.accountSetup(testName: String) {
    waitUntilExactlyOneExists(
        testName, hasText("next", ignoreCase = true)
                and hasAnyAncestor(hasTestTag("AccountSetupWizard"))
    )
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - AccountSetup - After Step 1")

    waitUntilExactlyOneExists(
        testName, hasText("next", ignoreCase = true)
                and hasAnyAncestor(hasTestTag("AccountSetupWizard"))
    )
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - AccountSetup - After Step 2")

    waitUntilExactlyOneExists(
        testName, hasText("next", ignoreCase = true)
                and hasAnyAncestor(hasTestTag("AccountSetupWizard"))
    )
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - AccountSetup - After Step 3")

    waitUntilExactlyOneExists(
        testName, hasText("next", ignoreCase = true)
                and hasAnyAncestor(hasTestTag("AccountSetupWizard"))
    )
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - AccountSetup - After Step 4")
}

private suspend fun ComposeUiTest.selfVerification(testName: String) {
    waitUntilExactlyOneExists(
        testName, hasText("next", ignoreCase = true)
                and hasAnyAncestor(hasTestTag("SelfVerificationWizard"))
    )
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - SelfVerification - Step 1")

    waitUntilExactlyOneExists(
        testName, hasText("next", ignoreCase = true)
                and hasAnyAncestor(hasTestTag("SelfVerificationWizard"))
    )
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - SelfVerification - Step 2")
}
