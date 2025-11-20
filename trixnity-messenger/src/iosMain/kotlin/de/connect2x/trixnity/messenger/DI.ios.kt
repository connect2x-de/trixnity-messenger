package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.uikit.ApplicationDelegate
import de.connect2x.trixnity.messenger.uikit.ApplicationDelegateProtocol
import de.connect2x.trixnity.messenger.uikit.ApplicationDelegateProxy
import de.connect2x.trixnity.messenger.uikit.WindowSceneDelegate
import de.connect2x.trixnity.messenger.uikit.WindowSceneDelegateProtocol
import de.connect2x.trixnity.messenger.uikit.WindowSceneDelegateProxy
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<WindowSceneDelegateProtocol>(named<UrlHandlingUIWindowSceneDelegate>()) {
        UrlHandlingUIWindowSceneDelegate
    }
}

fun delegateModule(): Module = module {
    single<ApplicationDelegateProtocol>(named<SceneConfigurationDelegate>()) {
        SceneConfigurationDelegate
    }

    single(createdAtStart = true) {
        ApplicationDelegate.delegate = ApplicationDelegateProxy(getAll<ApplicationDelegateProtocol>())
        WindowSceneDelegate.delegate = WindowSceneDelegateProxy(getAll<WindowSceneDelegateProtocol>())
    }
}
