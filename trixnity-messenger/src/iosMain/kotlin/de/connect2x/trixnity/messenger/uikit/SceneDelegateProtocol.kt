package de.connect2x.trixnity.messenger.uikit

import de.connect2x.trixnity.messenger.uikit.WithDefault.Default
import platform.Foundation.NSError
import platform.Foundation.NSUserActivity
import platform.UIKit.UIOpenURLContext
import platform.UIKit.UIScene
import platform.UIKit.UISceneConnectionOptions
import platform.UIKit.UISceneSession

/**
 * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiscenedelegate)
 */
interface SceneDelegateProtocol {

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiscenedelegate/scene(_:willconnectto:options:))
     */
    fun willConnect(
        scene: UIScene,
        session: UISceneSession,
        connectionOptions: UISceneConnectionOptions,
    ) { }

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiscenedelegate/scenediddisconnect(_:))
     */
    fun sceneDidDisconnect(
        scene: UIScene,
    ) { }

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiscenedelegate/scenewillenterforeground(_:))
     */
    fun sceneWillEnterForeground(
        scene: UIScene,
    ) { }

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiscenedelegate/scenedidbecomeactive(_:))
     */
    fun sceneDidBecomeActive(
        scene: UIScene,
    ) { }

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiscenedelegate/scenewillresignactive(_:))
     */
    fun sceneWillResignActive(
        scene: UIScene,
    ) { }

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiscenedelegate/scenedidenterbackground(_:))
     */
    fun sceneDidEnterBackground(
        scene: UIScene,
    ) { }

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiscenedelegate/scene(_:openurlcontexts:))
     */
    fun openUrlContexts(
        scene: UIScene,
        openUrlContexts: Set<UIOpenURLContext>,
    ) { }

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiscenedelegate/scene(_:willcontinueuseractivitywithtype:))
     */
    fun willContinueUserActivity(
        scene: UIScene,
        userActivityType: String,
    ) { }

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiscenedelegate/scene(_:continue:))
     */
    fun continueUserActivity(
        scene: UIScene,
        userActivity: NSUserActivity,
    ) { }

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiscenedelegate/scene(_:didfailtocontinueuseractivitywithtype:error:))
     */
    fun didFailToContinueUserActivity(
        scene: UIScene,
        userActivityType: String,
        error: NSError,
    ) { }

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiscenedelegate/staterestorationactivity(for:))
     */
    fun stateRestorationActivity(
        scene: UIScene,
    ): WithDefault<NSUserActivity?> = Default

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiscenedelegate/scene(_:restoreinteractionstatewith:))
     */
    fun restoreInteractionState(
        scene: UIScene,
        stateRestorationActivity: NSUserActivity,
    ) { }

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiscenedelegate/scene(_:didupdate:))
     */
    fun didUpdate(
        scene: UIScene,
        userActivity: NSUserActivity,
    ) { }
}
