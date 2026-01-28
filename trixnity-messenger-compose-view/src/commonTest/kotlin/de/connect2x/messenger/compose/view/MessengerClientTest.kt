package de.connect2x.messenger.compose.view

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.lognity.api.backend.Backend
import de.connect2x.lognity.test.TestBackend
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.profiles.IntroductionOrProfile
import de.connect2x.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test


@OptIn(ExperimentalTestApi::class)
class MessengerClientTest {

    init {
        Backend.setOnce(TestBackend)
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
                                IntroductionOrProfile()
                            }
                        }
                    },
                    activeMessenger = { matrixMessenger, rootViewModel ->
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

            // TODO assertions
        }
    }

    private fun runTestWithLogging(block: suspend TestScope.() -> Unit) = runTest {
        TestBackend.withTestScope {
            block()
        }
    }
}
