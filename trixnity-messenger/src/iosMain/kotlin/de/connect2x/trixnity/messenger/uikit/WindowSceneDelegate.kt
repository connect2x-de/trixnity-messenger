@file:OptIn(ExperimentalForeignApi::class, ExperimentalAtomicApi::class, BetaInteropApi::class)
@file:Suppress("UNUSED")

package de.connect2x.trixnity.messenger.uikit

import de.connect2x.trixnity.messenger.uikit.Utilities.delegate
import de.connect2x.trixnity.messenger.uikit.Utilities.unsafeCast
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSError
import platform.Foundation.NSUserActivity
import platform.UIKit.UIApplicationShortcutItem
import platform.UIKit.UICoordinateSpaceProtocol
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIScene
import platform.UIKit.UISceneConnectionOptions
import platform.UIKit.UISceneSession
import platform.UIKit.UITraitCollection
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.UIWindowSceneDelegateProtocol
import platform.UIKit.UIWindowSceneDelegateProtocolMeta
import platform.darwin.NSObject
import platform.darwin.NSObjectMeta

class WindowSceneDelegate : UIWindowSceneDelegateProtocol, NSObject {

    @OverrideInit constructor() : super()

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun window(): UIWindow? = delegate.window.orNull

    override fun setWindow(window: UIWindow?) {
        ApplicationDelegate.delegate.window = WithDefault.Value(window)
    }

    override fun windowScene(
        windowScene: UIWindowScene,
        didUpdateCoordinateSpace: UICoordinateSpaceProtocol,
        interfaceOrientation: UIInterfaceOrientation,
        traitCollection: UITraitCollection,
    ) =
        delegate.didUpdateCoordinateSpace(
            windowScene = windowScene,
            previousCoordinateSpace = didUpdateCoordinateSpace,
            previousInterfaceOrientation = interfaceOrientation,
            previousTraitCollection = traitCollection,
        )

    override fun windowScene(
        windowScene: UIWindowScene,
        performActionForShortcutItem: UIApplicationShortcutItem,
        completionHandler: (Boolean) -> Unit,
    ) =
        scope.delegate(completionHandler, { false }) {
            delegate.performAction(windowScene = windowScene, performActionForShortcutItem)
        }

    override fun windowScene(
        windowScene: UIWindowScene,
        userDidAcceptCloudKitShareWithMetadata: objcnames.classes.CKShareMetadata,
    ) =
        delegate.userDidAcceptCloudKitShare(
            windowScene = windowScene,
            cloudKitShareMetadata = userDidAcceptCloudKitShareWithMetadata.unsafeCast(),
        )

    override fun scene(scene: UIScene, willConnectToSession: UISceneSession, options: UISceneConnectionOptions) =
        delegate.willConnect(scene = scene, session = willConnectToSession, connectionOptions = options)

    override fun sceneDidDisconnect(scene: UIScene) = delegate.sceneDidDisconnect(scene = scene)

    override fun sceneWillEnterForeground(scene: UIScene) = delegate.sceneDidEnterBackground(scene = scene)

    override fun sceneDidBecomeActive(scene: UIScene) = delegate.sceneDidBecomeActive(scene = scene)

    override fun sceneWillResignActive(scene: UIScene) = delegate.sceneWillResignActive(scene = scene)

    override fun sceneDidEnterBackground(scene: UIScene) = delegate.sceneDidEnterBackground(scene = scene)

    override fun scene(scene: UIScene, openURLContexts: Set<*>) =
        delegate.openUrlContexts(scene = scene, openUrlContexts = openURLContexts.unsafeCast())

    override fun scene(scene: UIScene, willContinueUserActivityWithType: String) =
        delegate.willContinueUserActivity(scene = scene, userActivityType = willContinueUserActivityWithType)

    @ObjCSignatureOverride
    override fun scene(scene: UIScene, continueUserActivity: NSUserActivity) =
        delegate.continueUserActivity(scene = scene, userActivity = continueUserActivity)

    override fun scene(scene: UIScene, didFailToContinueUserActivityWithType: String, error: NSError) =
        delegate.didFailToContinueUserActivity(
            scene = scene,
            userActivityType = didFailToContinueUserActivityWithType,
            error = error,
        )

    override fun stateRestorationActivityForScene(scene: UIScene): NSUserActivity? =
        delegate.stateRestorationActivity(scene = scene).orNull

    @ObjCSignatureOverride
    override fun scene(scene: UIScene, restoreInteractionStateWithUserActivity: NSUserActivity) =
        delegate.restoreInteractionState(
            scene = scene,
            stateRestorationActivity = restoreInteractionStateWithUserActivity,
        )

    @ObjCSignatureOverride
    override fun scene(scene: UIScene, didUpdateUserActivity: NSUserActivity) =
        delegate.didUpdate(scene = scene, userActivity = didUpdateUserActivity)

    companion object : UIWindowSceneDelegateProtocolMeta, NSObjectMeta() {
        var delegate: WindowSceneDelegateProtocol
            get() = globalDelegate
            set(value) {
                globalDelegate = value
            }
    }
}

private var globalDelegate: WindowSceneDelegateProtocol = object : WindowSceneDelegateProtocol {}
