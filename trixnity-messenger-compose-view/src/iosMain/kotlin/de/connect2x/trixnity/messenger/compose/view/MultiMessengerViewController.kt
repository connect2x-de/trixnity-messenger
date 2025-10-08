package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import de.connect2x.trixnity.messenger.compose.view.profiles.Profiles
import de.connect2x.trixnity.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.trixnity.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.trixnity.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.trixnity.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.MatrixMultiMessengerService
import io.github.oshai.kotlinlogging.KotlinLogging
import platform.UIKit.UIViewController

private val log = KotlinLogging.logger {}

fun MultiMessengerViewController(lifecycle: Lifecycle): UIViewController {
    log.info { "Starting iOS client" }
    val matrixMultiMessenger = MatrixMultiMessengerService.get()
        ?: throw IllegalStateException("MatrixMultiMessengerService must be initialized")

    log.debug { "Created MatrixMultiMessenger" }

    return ComposeUIViewController(
        configure = { enforceStrictPlistSanityCheck = false }
    ) {
        WithProfileSelection(
            matrixMultiMessenger,
            componentContext = DefaultComponentContext(lifecycle),
            activeMessengerOnce = { _, _ -> },
            activeMessenger = { matrixMessenger, rootViewModel ->
                val isFocusHighlighting =
                    matrixMessenger.di.get<MatrixMessengerSettingsHolder>()
                        .collectAsState().value.base.isFocusHighlighting
                CompositionLocalProvider(
                    Platform provides PlatformType.IOS,
                    DI provides matrixMessenger.di,
                    IsFocusHighlighting provides isFocusHighlighting,
                ) {
                    MessengerTheme {
                        Client(rootViewModel)
                    }
                }
            },
            nonActiveMessenger = { existingProfiles ->
                val showProfileCreation = remember { mutableStateOf(false) }
                CompositionLocalProvider(
                    Platform provides PlatformType.IOS,
                    DI provides matrixMultiMessenger.di,
                    ShowProfileCreation provides showProfileCreation,
                    IsFocusHighlighting provides false,
                ) {
                    MessengerTheme {
                        Profiles(matrixMultiMessenger, existingProfiles)
                    }
                }
            }
        )
    }
}
