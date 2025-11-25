package de.connect2x.messenger.compose.view

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.arkivanov.essenty.lifecycle.Lifecycle
import de.connect2x.messenger.compose.view.profiles.Profiles
import de.connect2x.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.MatrixMultiMessengerService
import de.connect2x.trixnity.messenger.delegateModule
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.uikit.WindowSceneDelegateProtocol
import de.connect2x.trixnity.messenger.uikit.WithDefault
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.UIKit.UIScene
import platform.UIKit.UISceneConnectionOptions
import platform.UIKit.UISceneSession
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

private val log = KotlinLogging.logger {}

fun MatrixMultiMessengerConfiguration.addViewProvider() {
    modulesFactories += ::viewModule
    modulesFactories += ::delegateModule
}

private fun viewModule(): Module = module {
    single<ViewControllerFactory> {
        ViewControllerFactory {
            MultiMessengerViewController(it)
        }
    }
    single<WindowSceneDelegateProtocol>(named<WindowConnectingScene>()) {
        WindowConnectingScene(get())
    }
}


private class WindowConnectingScene(
    private val factory: ViewControllerFactory,
) : WindowSceneDelegateProtocol {

    private val lifecycle = ApplicationLifecycle()
    override var window: WithDefault<UIWindow?> = WithDefault.Value(null)

    override fun willConnect(
        scene: UIScene,
        session: UISceneSession,
        connectionOptions: UISceneConnectionOptions,
    ) {
        val windowScene = scene as? UIWindowScene ?: return
        val newWindow = UIWindow(windowScene = windowScene)
        val rootViewController = factory(lifecycle)
        newWindow.rootViewController = rootViewController
        newWindow.makeKeyAndVisible()
        window = WithDefault.Value(newWindow)
    }
}

private fun MultiMessengerViewController(lifecycle: Lifecycle): UIViewController {
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
