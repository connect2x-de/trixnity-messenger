package de.connect2x.trixnity.messenger

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import platform.UIKit.UIScene
import platform.UIKit.UISceneConnectionOptions
import platform.UIKit.UISceneSession
import platform.UIKit.UIWindowSceneDelegateProtocol
import platform.darwin.NSObject

/**
 * This allows to proxy calls to delegates. Not all methods are implemented. If you need more, add it.
 */
@OptIn(ExperimentalForeignApi::class)
abstract class UIWindowSceneDelegateProxy(
    private val delegates: List<UIWindowSceneDelegateProtocol>
) : UIWindowSceneDelegateProtocol, NSObject() {
    override fun scene(
        scene: UIScene,
        didFailToContinueUserActivityWithType: String,
        error: NSError
    ) {
        delegates.forEach { it.scene(scene, didFailToContinueUserActivityWithType, error) }
    }

    override fun scene(scene: UIScene, willContinueUserActivityWithType: String) {
        delegates.forEach { it.scene(scene, willContinueUserActivityWithType) }
    }

    override fun scene(
        scene: UIScene,
        willConnectToSession: UISceneSession,
        options: UISceneConnectionOptions
    ) {
        delegates.forEach { it.scene(scene, willConnectToSession, options) }
    }

    override fun sceneDidBecomeActive(scene: UIScene) {
        delegates.forEach { it.sceneDidBecomeActive(scene) }
    }

    override fun sceneDidDisconnect(scene: UIScene) {
        delegates.forEach { it.sceneDidDisconnect(scene) }
    }

    override fun sceneDidEnterBackground(scene: UIScene) {
        delegates.forEach { it.sceneDidEnterBackground(scene) }
    }

    override fun sceneWillEnterForeground(scene: UIScene) {
        delegates.forEach { it.sceneWillEnterForeground(scene) }
    }

    override fun sceneWillResignActive(scene: UIScene) {
        delegates.forEach { it.sceneWillResignActive(scene) }
    }
}
