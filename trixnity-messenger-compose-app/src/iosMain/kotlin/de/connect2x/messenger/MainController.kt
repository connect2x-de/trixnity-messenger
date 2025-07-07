package de.connect2x.messenger

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import de.connect2x.messenger.compose.view.Client
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.IsFocused
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.PlatformType
import de.connect2x.messenger.compose.view.profiles.Profiles
import de.connect2x.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.create
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import platform.UIKit.UIViewController

private val log = KotlinLogging.logger {}

fun MainViewController(lifecycle: Lifecycle): UIViewController {
    log.info { "Starting iOS client" }
    val matrixMultiMessenger = runBlocking {
        MatrixMultiMessenger.create(
            configuration = messengerConfiguration()
        )
    }
    log.debug { "Created MatrixMultiMessenger" }

    return ComposeUIViewController(
        configure = { enforceStrictPlistSanityCheck = false }
    ) {
        WithProfileSelection(
            matrixMultiMessenger,
            componentContext = DefaultComponentContext(lifecycle),
            activeMessengerOnce = { _, _ ->
                // TODO
            },
            activeMessenger = { matrixMessenger, rootViewModel ->
                CompositionLocalProvider(
                    Platform provides PlatformType.IOS, // TODO iOS
                    IsFocused provides true, // TODO
                    DI provides matrixMessenger.di,
                ) {
                    MessengerTheme {
                        Client(rootViewModel)
                    }
                }
            },
            nonActiveMessenger = { existingProfiles ->
                val showProfileCreation = remember { mutableStateOf(false) }
                CompositionLocalProvider(
                    Platform provides PlatformType.IOS, // TODO
                    IsFocused provides true, // TODO
                    DI provides matrixMultiMessenger.di,
                    ShowProfileCreation provides showProfileCreation,
                ) {
                    MessengerTheme {
                        Profiles(matrixMultiMessenger, existingProfiles)
                    }
                }
            }
        )
    }
}
