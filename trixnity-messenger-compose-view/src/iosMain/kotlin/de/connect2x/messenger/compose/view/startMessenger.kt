package de.connect2x.messenger.compose.view

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.start
import com.arkivanov.essenty.lifecycle.stop
import de.connect2x.messenger.compose.view.profiles.Profiles
import de.connect2x.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.create
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIViewController

private val log = KotlinLogging.logger {}

fun startMessenger(
    lifecycle: LifecycleRegistry,
    configuration: MatrixMultiMessengerConfiguration.() -> Unit
): UIViewController {
    log.info { "Starting iOS client" }
    val matrixMultiMessenger = runBlocking {
        MatrixMultiMessenger.create(configuration = configuration)
    }
    log.debug { "Created MatrixMultiMessenger" }

    return ComposeUIViewController(
        configure = { enforceStrictPlistSanityCheck = false }
    ) {
        var isFocused by remember { mutableStateOf(false) }

        WithProfileSelection(
            matrixMultiMessenger,
            componentContext = DefaultComponentContext(lifecycle),
            activeMessengerOnce = { _, _ ->
                val notificationCenter = NSNotificationCenter.defaultCenter()
                notificationCenter.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, null) { _ ->
                    isFocused = false
                    lifecycle.stop()
                }
                notificationCenter.addObserverForName(UIApplicationWillEnterForegroundNotification, null, null) { _ ->
                    isFocused = true
                    lifecycle.start()
                }
            },
            activeMessenger = { matrixMessenger, rootViewModel ->
                val isFocusHighlighting =
                    matrixMessenger.di.get<MatrixMessengerSettingsHolder>()
                        .collectAsState().value.base.isFocusHighlighting
                CompositionLocalProvider(
                    Platform provides PlatformType.IOS,
                    IsFocused provides isFocused,
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
                    IsFocused provides isFocused,
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
