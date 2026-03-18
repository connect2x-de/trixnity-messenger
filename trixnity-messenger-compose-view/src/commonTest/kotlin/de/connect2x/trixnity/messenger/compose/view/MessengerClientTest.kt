package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.lognity.api.backend.Backend
import de.connect2x.lognity.test.TestBackend
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.messenger.createUser
import de.connect2x.trixnity.messenger.compose.view.messenger.login
import de.connect2x.trixnity.messenger.compose.view.profiles.Profiles
import de.connect2x.trixnity.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.trixnity.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.trixnity.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.trixnity.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.compose.view.util.generateUsername
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.update
import kotlinx.coroutines.currentCoroutineContext
import kotlin.test.Test
import kotlin.uuid.Uuid


@OptIn(ExperimentalTestApi::class)
class MessengerClientTest {

    init {
        Backend.set(TestBackend)
    }

    @Test
    fun messengerClientComposableLoadsSuccessfully() = runComposeUiTest {
        val matrixMultiMessenger = createTestMatrixMultiMessenger(currentCoroutineContext())
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
                        matrixMessenger.di.get<MatrixMessengerSettingsHolder>()
                            .update<MatrixMessengerSettingsBase> {
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

        val testName = "messengerClientComposableLoadsSuccessfully"
        val username = generateUsername()
        val password = Uuid.generateV4().toString()

        createUser(username, password)
        login(testName, username, password)
    }
}
