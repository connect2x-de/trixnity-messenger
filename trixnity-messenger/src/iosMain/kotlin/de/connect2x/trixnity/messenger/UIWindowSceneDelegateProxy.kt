package de.connect2x.trixnity.messenger

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import platform.UIKit.UIScene
import platform.UIKit.UISceneConnectionOptions
import platform.UIKit.UISceneSession
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowSceneDelegateProtocol
import platform.darwin.NSObject

interface KUIWindowSceneDelegateProtocol {

    fun window(): UIWindow? = null
    fun setWindow(window: UIWindow?) { }

    fun scene(
        scene: UIScene,
        didFailToContinueUserActivityWithType: String,
        error: NSError
    ) { }

    fun scene(scene: UIScene, willContinueUserActivityWithType: String) {}

    fun scene(
        scene: UIScene,
        willConnectToSession: UISceneSession,
        options: UISceneConnectionOptions
    ) {}

    fun sceneDidBecomeActive(scene: UIScene) { }

    fun sceneDidDisconnect(scene: UIScene) { }

    fun sceneDidEnterBackground(scene: UIScene) { }

    fun sceneWillEnterForeground(scene: UIScene) { }

    fun sceneWillResignActive(scene: UIScene) { }

    fun scene(scene: UIScene, openURLContexts: Set<*>) { }
}

class UIWindowSceneDelegateProtocolWrapper<T : KUIWindowSceneDelegateProtocol>(
    val inner: T
) : UIWindowSceneDelegateProtocol, NSObject() {
    override fun scene(
        scene: UIScene,
        didFailToContinueUserActivityWithType: String,
        error: NSError
    ) = inner.scene(scene = scene, didFailToContinueUserActivityWithType = didFailToContinueUserActivityWithType, error = error)

    override fun scene(scene: UIScene, willContinueUserActivityWithType: String)
        = inner.scene(scene = scene, willContinueUserActivityWithType = willContinueUserActivityWithType)

    override fun scene(
        scene: UIScene,
        willConnectToSession: UISceneSession,
        options: UISceneConnectionOptions
    ) =
        inner.scene(scene = scene, willConnectToSession = willConnectToSession, options = options)

    override fun scene(scene: UIScene, openURLContexts: Set<*>)
            = inner.scene(scene = scene, openURLContexts = openURLContexts)

    override fun sceneDidBecomeActive(scene: UIScene)
        = inner.sceneDidBecomeActive(scene = scene)

    override fun sceneDidDisconnect(scene: UIScene) = inner.sceneDidDisconnect(scene = scene)

    override fun sceneDidEnterBackground(scene: UIScene) = inner.sceneDidEnterBackground(scene = scene)

    override fun sceneWillEnterForeground(scene: UIScene) = inner.sceneWillEnterForeground(scene = scene)

    override fun sceneWillResignActive(scene: UIScene) = inner.sceneWillResignActive(scene = scene)
}

fun KUIWindowSceneDelegateProtocol.toWrapped()
    = UIWindowSceneDelegateProtocolWrapper(this)


/**
 * This allows to proxy calls to delegates. Not all methods are implemented. If you need more, add it.
 */
@OptIn(ExperimentalForeignApi::class)
abstract class UIWindowSceneDelegateProxy(
    private val delegates: List<KUIWindowSceneDelegateProtocol>
) : KUIWindowSceneDelegateProtocol {

    override fun window(): UIWindow? = delegates.firstNotNullOfOrNull { it.window() }
    override fun setWindow(window: UIWindow?) { delegates.forEach { it.setWindow(window) } }

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
        delegates.forEach { it.scene(scene, openURLContexts)}
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
