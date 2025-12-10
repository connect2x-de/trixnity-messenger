package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.uikit.WindowSceneDelegateProtocol
import de.connect2x.trixnity.messenger.util.UriHandlerImpl
import kotlinx.coroutines.runBlocking
import platform.UIKit.UIOpenURLContext
import platform.UIKit.UIScene

class UrlHandlingUIWindowSceneDelegate(
    private val urlHandler: UriHandlerImpl
) : WindowSceneDelegateProtocol {
    override fun openUrlContexts(scene: UIScene, openUrlContexts: Set<UIOpenURLContext>) {
        val uri = openUrlContexts.firstOrNull()?.URL?.absoluteString
        if (uri != null) {
            runBlocking { urlHandler.onUri(uri) }
        }
    }
}
