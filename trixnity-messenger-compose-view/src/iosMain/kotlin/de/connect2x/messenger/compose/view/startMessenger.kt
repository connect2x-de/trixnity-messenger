package de.connect2x.messenger.compose.view

import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.start
import com.arkivanov.essenty.lifecycle.stop
import de.connect2x.messenger.compose.view.notifications.NotificationHandlerProvider
import de.connect2x.messenger.compose.view.profiles.Profiles
import de.connect2x.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.create
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIViewController
import platform.UIKit.registerForRemoteNotifications
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private val log = KotlinLogging.logger {}

class MatrixMultiMessengerHolder {
    private val mutex = Mutex()
    private var matrixMultiMessenger: MatrixMultiMessenger? = null

    suspend fun getOrCreate(factory: suspend () -> MatrixMultiMessenger): MatrixMultiMessenger = mutex.withLock {
        if (matrixMultiMessenger == null) {
            matrixMultiMessenger = factory()
            return@withLock checkNotNull(matrixMultiMessenger)
        }

        return checkNotNull(matrixMultiMessenger)
    }
}

// Because the notification delegate can be called when the other messenger is running, we should ensure we are only
// using one messenger at the time.
internal val multiMessengerHolder: MatrixMultiMessengerHolder = MatrixMultiMessengerHolder()

fun startMessenger(
    lifecycle: LifecycleRegistry,
    configuration: MatrixMultiMessengerConfiguration.() -> Unit
): UIViewController {
    log.info { "Starting iOS client" }
    val matrixMultiMessenger = runBlocking {
        multiMessengerHolder.getOrCreate {
            MatrixMultiMessenger.create(configuration)
        }
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

                val scope = matrixMultiMessenger.di.get<CoroutineScope>()
                scope.launch {
                    matrixMultiMessenger.activeMatrixMessenger.filterNotNull().collectLatest { matrixMessenger ->
                        val notificationHandlerProvider = matrixMessenger.di.get<NotificationHandlerProvider>()
                        matrixMessenger.di.get<MatrixMessengerSettingsHolder>()
                            .map { it.base.accounts }
                            .distinctUntilChanged()
                            .conflate()
                            .collect { multiSettings ->
                                for ((userId, settings) in multiSettings) {
                                    log.trace { "Checking notification permission for $userId" }
                                    if (!settings.base.notificationsEnabled) {
                                        continue
                                    }

                                    log.trace { "Request notification permissions for $userId" }
                                    notificationHandlerProvider(userId.toString()).requestPermissions { _, granted ->
                                        if (granted) {
                                            log.debug {
                                                "Granted permissions for notifications, add pushers... (userId=$userId)"
                                            }
                                            dispatch_async(dispatch_get_main_queue()) {
                                                UIApplication.sharedApplication.registerForRemoteNotifications()
                                            }
                                            scope.launch {
                                                setPusher(matrixMessenger, userId)
                                            }
                                        }
                                    }
                                }
                            }
                    }
                }
            },
            activeMessenger = { matrixMessenger, rootViewModel ->
                CompositionLocalProvider(
                    Platform provides PlatformType.IOS,
                    IsFocused provides isFocused,
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
