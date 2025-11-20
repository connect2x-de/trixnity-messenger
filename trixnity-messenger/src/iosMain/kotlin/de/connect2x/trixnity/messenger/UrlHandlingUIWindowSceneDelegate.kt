package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.uikit.WindowSceneDelegateProtocol
import de.connect2x.trixnity.messenger.util.defaultUrlHandler
import kotlinx.coroutines.runBlocking
import platform.UIKit.UIOpenURLContext
import platform.UIKit.UIScene

object UrlHandlingUIWindowSceneDelegate : WindowSceneDelegateProtocol {
    override fun openUrlContexts(scene: UIScene, openUrlContexts: Set<UIOpenURLContext>) {
        val uri = openUrlContexts.firstOrNull()?.URL?.absoluteString
        if (uri != null) {
            runBlocking {
                withMatrixMessengerFromService {
                    it.defaultUrlHandler.onUri(uri)
                }
            }
        }
    }
}
