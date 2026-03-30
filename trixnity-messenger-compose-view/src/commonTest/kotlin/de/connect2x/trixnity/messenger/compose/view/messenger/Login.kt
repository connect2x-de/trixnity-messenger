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
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.PlatformType
import de.connect2x.trixnity.messenger.compose.view.platformType
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

private fun getUrl(): String {
    return if (platformType() == PlatformType.ANDROID) "http://10.0.2.2:8008"
    else "http://localhost:8008"
}

private suspend fun ComposeUiTest.serverLogin(
    testName: String,
    username: String,
    password: String,
) {
    screenshot(testName, "Login - Login - Before Server Discovery")
    waitUntilExactlyOneExists(testName, hasText("Your Matrix Server", ignoreCase = true)).assertExists()
        .performTextInput(getUrl())

    waitUntilExactlyOneExists(testName, hasText("Login With Password", ignoreCase = true))
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - Login - Username+Password", surface = "ConnectingWizard")
    onNodeWithText("Your Matrix Username", ignoreCase = true)
        .performTextInput(username)
    onNodeWithText("Your password", ignoreCase = true)
        .performTextInput(password)
    waitForIdle()
    waitUntilExactlyOneExists(testName, hasText("Login", ignoreCase = true))
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - Login - After Login", surface = "ConnectingWizard")
}

private suspend fun ComposeUiTest.vault(testName: String) {
    waitUntilExactlyOneExists(testName, hasText("create vault", ignoreCase = true))
    screenshot(testName, "Login - Vault - Create", surface = "CrossSigningBootstrapWizard")
    onNodeWithText("create vault", ignoreCase = true).performClick()
    waitForIdle()

    waitUntilExactlyOneExists(
        testName,
        hasText("I have copied the recovery key", substring = true, ignoreCase = true),
        timeoutMillis = 60_000, // on JS/WASM in the CI, this can take a while
    )
    screenshot(testName, "Login - Vault - Created", surface = "CrossSigningBootstrapWizard")

    onNodeWithText("I have copied the recovery key", substring = true, ignoreCase = true)
        .performClick()
    waitUntilExactlyOneExists(
        testName, hasText("next", ignoreCase = true)
                and isEnabled()
                and hasAnyAncestor(hasTestTag("CrossSigningBootstrapWizard"))
    )
        .performClick()

    screenshot(testName, "Login - Vault - Finished", surface = "CrossSigningBootstrapWizard")
    waitUntilExactlyOneExists(testName, hasText("confirm", ignoreCase = true))
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - Vault - Confirmed", surface = "CrossSigningBootstrapWizard")
}

private suspend fun ComposeUiTest.accountSetup(testName: String) {
    waitUntilExactlyOneExists(
        testName, hasText("next", ignoreCase = true)
                and hasAnyAncestor(hasTestTag("AccountSetupWizard"))
    )
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - AccountSetup - After Step 1", surface = "AccountSetupWizard")

    waitUntilExactlyOneExists(
        testName, hasText("next", ignoreCase = true)
                and hasAnyAncestor(hasTestTag("AccountSetupWizard"))
    )
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - AccountSetup - After Step 2", surface = "AccountSetupWizard")

    waitUntilExactlyOneExists(
        testName, hasText("next", ignoreCase = true)
                and hasAnyAncestor(hasTestTag("AccountSetupWizard"))
    )
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - AccountSetup - After Step 3", surface = "AccountSetupWizard")

    waitUntilExactlyOneExists(
        testName, hasText("next", ignoreCase = true)
                and hasAnyAncestor(hasTestTag("AccountSetupWizard"))
    )
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - AccountSetup - After Step 4", surface = "AccountSetupWizard")
}

private suspend fun ComposeUiTest.selfVerification(testName: String) {
    waitUntilExactlyOneExists(
        testName, hasText("next", ignoreCase = true)
                and hasAnyAncestor(hasTestTag("SelfVerificationWizard"))
    )
        .performClick()
    waitForIdle()
    screenshot(testName, "Login - SelfVerification") // should be done here
}
