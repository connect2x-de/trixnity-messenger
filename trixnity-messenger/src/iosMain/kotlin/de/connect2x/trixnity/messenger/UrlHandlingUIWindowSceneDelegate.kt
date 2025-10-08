package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.defaultUrlHandler
import kotlinx.coroutines.runBlocking
import platform.UIKit.UIOpenURLContext
import platform.UIKit.UIScene
import platform.UIKit.UIWindowSceneDelegateProtocol
import platform.darwin.NSObject

class UrlHandlingUIWindowSceneDelegate : UIWindowSceneDelegateProtocol, NSObject() {
    override fun scene(scene: UIScene, openURLContexts: Set<*>) {
        val uri = (openURLContexts.firstOrNull() as? UIOpenURLContext)?.URL?.absoluteString
        if (uri != null) {
            runBlocking {
                withMatrixMessengerFromService {
                    it.defaultUrlHandler.onUri(uri)
                }
            }
        }
    }
}
