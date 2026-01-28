package de.connect2x.trixnity.messenger.uikit

import platform.CloudKit.CKShareMetadata
import platform.UIKit.UIApplicationShortcutItem
import platform.UIKit.UICoordinateSpaceProtocol
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UITraitCollection
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

open class WindowSceneDelegateProxy<T : WindowSceneDelegateProtocol>(
    delegates: List<T>
) : SceneDelegateProxy<T>(delegates), WindowSceneDelegateProtocol {

    override var window: WithDefault<UIWindow?>
        get() = delegateDefault { window }
        set(value) {
            delegate { window = value }
        }

    override suspend fun performAction(
        windowScene: UIWindowScene,
        shortcutItem: UIApplicationShortcutItem,
    ): WithDefault<Boolean> = delegateDefaultSuspend {
        performAction(
            windowScene = windowScene,
            shortcutItem = shortcutItem,
        )
    }

    override fun userDidAcceptCloudKitShare(
        windowScene: UIWindowScene,
        cloudKitShareMetadata: CKShareMetadata,
    ) = delegate {
        userDidAcceptCloudKitShare(
            windowScene = windowScene,
            cloudKitShareMetadata = cloudKitShareMetadata,
        )
    }

    override fun didUpdateCoordinateSpace(
        windowScene: UIWindowScene,
        previousCoordinateSpace: UICoordinateSpaceProtocol,
        previousInterfaceOrientation: UIInterfaceOrientation,
        previousTraitCollection: UITraitCollection,
    ) = delegate {
        didUpdateCoordinateSpace(
            windowScene = windowScene,
            previousCoordinateSpace = previousCoordinateSpace,
            previousInterfaceOrientation = previousInterfaceOrientation,
            previousTraitCollection = previousTraitCollection,
        )
    }

}
