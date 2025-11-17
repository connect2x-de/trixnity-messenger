package de.connect2x.trixnity.messenger

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import platform.UIKit.UIScene
import platform.UIKit.UISceneConnectionOptions
import platform.UIKit.UISceneSession
import platform.UIKit.UIWindow

interface KUIWindowSceneDelegateProtocol {

    fun window(): UIWindow? = null
    fun setWindow(window: UIWindow?) {}

    fun scene(
        scene: UIScene,
        didFailToContinueUserActivityWithType: String,
        error: NSError
    ) {
    }

    fun scene(scene: UIScene, willContinueUserActivityWithType: String) {}

    fun scene(
        scene: UIScene,
        willConnectToSession: UISceneSession,
        options: UISceneConnectionOptions
    ) {
    }

    fun sceneDidBecomeActive(scene: UIScene) {}

    fun sceneDidDisconnect(scene: UIScene) {}

    fun sceneDidEnterBackground(scene: UIScene) {}

    fun sceneWillEnterForeground(scene: UIScene) {}

    fun sceneWillResignActive(scene: UIScene) {}

    fun scene(scene: UIScene, openURLContexts: Set<*>) {}
}

/**
 * This allows to proxy calls to delegates. Not all methods are implemented. If you need more, add it.
 */
@OptIn(ExperimentalForeignApi::class)
abstract class UIWindowSceneDelegateProxy(
    private val delegates: List<KUIWindowSceneDelegateProtocol>
) : KUIWindowSceneDelegateProtocol {

    override fun window(): UIWindow? = delegates.firstNotNullOfOrNull { it.window() }
    override fun setWindow(window: UIWindow?) {
        delegates.forEach { it.setWindow(window) }
    }

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

    override fun scene(scene: UIScene, openURLContexts: Set<*>) {
        delegates.forEach { it.scene(scene, openURLContexts) }
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
