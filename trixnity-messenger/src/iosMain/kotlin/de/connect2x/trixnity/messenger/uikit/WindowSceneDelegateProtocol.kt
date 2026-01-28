package de.connect2x.trixnity.messenger.uikit

import de.connect2x.trixnity.messenger.uikit.WithDefault.Default
import platform.CloudKit.CKShareMetadata
import platform.UIKit.UIApplicationShortcutItem
import platform.UIKit.UICoordinateSpaceProtocol
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UITraitCollection
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/**
 * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiwindowscenedelegate)
 */
interface WindowSceneDelegateProtocol : SceneDelegateProtocol {

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiwindowscenedelegate/window)
     */
    var window: WithDefault<UIWindow?>
        get() = Default
        set(value) {}

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiwindowscenedelegate/windowscene(_:performactionfor:completionhandler:))
     */
    suspend fun performAction(
        windowScene: UIWindowScene,
        shortcutItem: UIApplicationShortcutItem,
    ): WithDefault<Boolean> = Default

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiwindowscenedelegate/windowscene(_:userdidacceptcloudkitsharewith:))
     */
    fun userDidAcceptCloudKitShare(
        windowScene: UIWindowScene,
        cloudKitShareMetadata: CKShareMetadata,
    ) {
    }

    /**
     * [Apple Documentation](https://developer.apple.com/documentation/uikit/uiwindowscenedelegate/windowscene(_:didupdate:interfaceorientation:traitcollection:))
     */
    fun didUpdateCoordinateSpace(
        windowScene: UIWindowScene,
        previousCoordinateSpace: UICoordinateSpaceProtocol,
        previousInterfaceOrientation: UIInterfaceOrientation,
        previousTraitCollection: UITraitCollection,
    ) {
    }
}
