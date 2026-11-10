package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.uikit.ApplicationDelegate
import de.connect2x.trixnity.messenger.uikit.ApplicationDelegateProtocol
import de.connect2x.trixnity.messenger.uikit.ApplicationDelegateProxy
import de.connect2x.trixnity.messenger.uikit.WindowSceneDelegate
import de.connect2x.trixnity.messenger.uikit.WindowSceneDelegateProtocol
import de.connect2x.trixnity.messenger.uikit.WindowSceneDelegateProxy
import de.connect2x.trixnity.messenger.util.UriHandler
import de.connect2x.trixnity.messenger.util.UriHandlerImpl
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

suspend fun MatrixMessenger.Companion.create(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    configuration: MatrixMessengerConfiguration.() -> Unit = {},
): MatrixMessenger =
    MatrixMessengerImpl(
        coroutineContext = coroutineContext,
        configuration = {
            modulesFactories += {
                module {
                    single<WindowSceneDelegateProtocol>(named<UrlHandlingUIWindowSceneDelegate>()) {
                        UrlHandlingUIWindowSceneDelegate(
                            checkNotNull(get<UriHandler>() as? UriHandlerImpl) {
                                "default UrlHandler has been overridden and is not of expected type UrlHandlerImpl"
                            }
                        )
                    }
                }
            }
            configuration()
        },
    )

fun delegateModule(): Module = module {
    single<ApplicationDelegateProtocol>(named<SceneConfigurationDelegate>()) { SceneConfigurationDelegate }

    single(createdAtStart = true) {
        ApplicationDelegate.delegate = ApplicationDelegateProxy(getAll<ApplicationDelegateProtocol>())
        WindowSceneDelegate.delegate = WindowSceneDelegateProxy(getAll<WindowSceneDelegateProtocol>())
    }
}
