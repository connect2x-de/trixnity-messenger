package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.lognity.api.backend.Backend
import de.connect2x.lognity.test.TestBackend
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.profiles.Profiles
import de.connect2x.trixnity.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.trixnity.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.trixnity.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.trixnity.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test


@OptIn(ExperimentalTestApi::class)
class MessengerClientTest {

    init {
        Backend.set(TestBackend)
    }

    @Test
    fun messengerClientComposableLoadsSuccessfully() = runTestWithLogging {
        val matrixMultiMessenger = createTestMatrixMultiMessenger()
        runComposeUiTest {
            val lifecycle = LifecycleRegistry()
            setContent {
                WithProfileSelection(
                    matrixMultiMessenger = matrixMultiMessenger,
                    componentContext = DefaultComponentContext(lifecycle),
                    activeMessengerOnce = { _, _ -> },
                    nonActiveMessenger = {
                        val showProfileCreation = remember { mutableStateOf(false) }
                        CompositionLocalProvider(
                            Platform provides platformType(),
                            DI provides matrixMultiMessenger.di,
                            ShowProfileCreation provides showProfileCreation,
                            IsFocusHighlighting provides false,
                        ) {
                            MessengerTheme {
                                Profiles()
                            }
                        }
                    },
                    activeMessenger = { matrixMessenger, rootViewModel ->
                        LaunchedEffect(Unit) {
                            matrixMessenger.di.get<MatrixMessengerSettingsHolder>().update<MatrixMessengerSettingsBase> {
                                it.copy(preferredLang = "EN")
                            }
                        }
                        CompositionLocalProvider(
                            Platform provides platformType(),
                            DI provides matrixMessenger.di,
                            IsFocusHighlighting provides false,
                        ) {
                            MessengerTheme {
                                Client(rootViewModel)
                            }
                        }
                    })
            }

            waitForIdle()
            matrixMultiMessenger.di.get<I18nView>()
            matrixMultiMessenger.di.get<MatrixMultiMessengerConfiguration>()

            // use this to screenshot the current screen:    onNodeWithTag("ClientSurface").screenshot("root.png")
            onNodeWithText("Your Matrix Server", ignoreCase = true).assertExists().performTextInput("http://localhost:8008")
            waitUntilAtLeastOneExists(hasText("Login With Password", ignoreCase = true), timeoutMillis = 5_000L)
            onNodeWithText("Login With Password", ignoreCase = true).performClick()
            waitForIdle()
            onNodeWithText("Your Matrix Username", ignoreCase = true).performTextInput("testuser")
            onNodeWithText("Your password", ignoreCase = true).performTextInput("testpassword")
            waitForIdle()
            onNodeWithText("Login", ignoreCase = true).performClick()
            waitForIdle()

            waitUntilExactlyOneExists(hasText("next"), timeoutMillis = 5_000L)
            onNodeWithText("next", ignoreCase = true).performClick()
            waitForIdle()

            waitUntilExactlyOneExists(hasText("create vault", ignoreCase = true), timeoutMillis = 5_000L)
            onNodeWithText("create vault", ignoreCase = true).performClick()
            waitForIdle()

            onNodeWithTag("ClientSurface").screenshot("Vault.png")

            // TODO assertions
        }
    }

    private fun runTestWithLogging(block: suspend TestScope.() -> Unit) = runTest {
        TestBackend.withTestScope {
            block()
        }
    }
}
