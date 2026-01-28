@file:OptIn(ExperimentalForeignApi::class, ExperimentalAtomicApi::class, BetaInteropApi::class)
@file:Suppress("UNUSED")

package de.connect2x.trixnity.messenger.uikit

import de.connect2x.trixnity.messenger.uikit.Utilities.delegate
import de.connect2x.trixnity.messenger.uikit.Utilities.unsafeCast
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import objcnames.classes.INIntent
import platform.Foundation.NSCoder
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDelegateProtocol
import platform.UIKit.UIApplicationDelegateProtocolMeta
import platform.UIKit.UIApplicationExtensionPointIdentifier
import platform.UIKit.UIBackgroundFetchResult
import platform.UIKit.UIBackgroundFetchResult.UIBackgroundFetchResultNoData
import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UIInterfaceOrientationMaskAll
import platform.UIKit.UISceneConfiguration
import platform.UIKit.UISceneConnectionOptions
import platform.UIKit.UISceneSession
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.darwin.NSObjectMeta
import kotlin.concurrent.atomics.ExperimentalAtomicApi


class ApplicationDelegate : UIApplicationDelegateProtocol, NSObject {

    @OverrideInit
    constructor() : super()

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun window(): UIWindow? = delegate.window.orNull

    override fun setWindow(
        window: UIWindow?,
    ) {
        delegate.window = WithDefault.Value(window)
    }

    @ObjCSignatureOverride
    override fun application(
        application: UIApplication,
        willFinishLaunchingWithOptions: Map<Any?, *>?,
    ): Boolean = delegate.willFinishLaunching(
        application = application,
        launchOptions = willFinishLaunchingWithOptions.unsafeCast(),
    ).orTrue

    @ObjCSignatureOverride
    override fun application(
        application: UIApplication,
        didFinishLaunchingWithOptions: Map<Any?, *>?,
    ): Boolean = delegate.didFinishLaunching(
        application = application,
        launchOptions = didFinishLaunchingWithOptions.unsafeCast(),
    ).orTrue

    override fun application(
        application: UIApplication,
        configurationForConnectingSceneSession: UISceneSession,
        options: UISceneConnectionOptions,
    ): UISceneConfiguration = delegate.configurationForConnecting(
        application = application,
        sceneSession = configurationForConnectingSceneSession,
        options = options,
    ).valueOr { error("No scene available") }

    override fun application(
        application: UIApplication,
        didDiscardSceneSessions: Set<*>,
    ) = delegate.didDiscardSceneSessions(
        application = application,
        sceneSessions = didDiscardSceneSessions.unsafeCast(),
    )

    override fun applicationWillTerminate(
        application: UIApplication,
    ) = delegate.willTerminate(
        application = application,
    )

    override fun applicationProtectedDataDidBecomeAvailable(
        application: UIApplication,
    ) = delegate.protectedDataDidBecomeAvailable(
        application = application,
    )

    override fun applicationProtectedDataWillBecomeUnavailable(
        application: UIApplication,
    ) = delegate.protectedDataWillBecomeUnavailable(
        application = application,
    )

    override fun applicationDidReceiveMemoryWarning(
        application: UIApplication,
    ) = delegate.didReceiveMemoryWarning(
        application = application,
    )

    override fun applicationSignificantTimeChange(
        application: UIApplication,
    ) = delegate.significantTimeChange(
        application = application,
    )

    @ObjCSignatureOverride
    override fun application(
        application: UIApplication,
        shouldSaveSecureApplicationState: NSCoder,
    ): Boolean = delegate.shouldSaveSecureApplicationState(
        application = application,
        coder = shouldSaveSecureApplicationState,
    ).orFalse

    @ObjCSignatureOverride
    override fun application(
        application: UIApplication,
        shouldRestoreSecureApplicationState: NSCoder,
    ): Boolean = delegate.shouldRestoreSecureApplicationState(
        application = application,
        coder = shouldRestoreSecureApplicationState,
    ).orTrue

    override fun application(
        application: UIApplication,
        viewControllerWithRestorationIdentifierPath: List<*>,
        coder: NSCoder,
    ): UIViewController? = delegate.viewController(
        application = application,
        identifierComponents = viewControllerWithRestorationIdentifierPath.unsafeCast(),
        coder = coder,
    ).orNull

    @ObjCSignatureOverride
    override fun application(
        application: UIApplication,
        willEncodeRestorableStateWithCoder: NSCoder,
    ) = delegate.willEncodeRestorableState(
        application = application,
        coder = willEncodeRestorableStateWithCoder,
    )

    @ObjCSignatureOverride
    override fun application(
        application: UIApplication,
        didDecodeRestorableStateWithCoder: NSCoder,
    ) = delegate.didDecodeRestorableState(
        application = application,
        coder = didDecodeRestorableStateWithCoder,
    )

    override fun application(
        application: UIApplication,
        handleEventsForBackgroundURLSession: String,
        completionHandler: () -> Unit,
    ) = scope.delegate(completionHandler) {
        delegate.handleEventsForBackgroundURLSession(
            application = application,
            identifier = handleEventsForBackgroundURLSession,
        )
    }

    override fun application(
        application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken: NSData,
    ) = delegate.didRegisterForRemoteNotifications(
        application = application,
        deviceToken = didRegisterForRemoteNotificationsWithDeviceToken,
    )

    override fun application(
        application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError: NSError,
    ) = delegate.didFailToRegisterForRemoteNotifications(
        application = application,
        error = didFailToRegisterForRemoteNotificationsWithError,
    )

    override fun application(
        application: UIApplication,
        didReceiveRemoteNotification: Map<Any?, *>,
        fetchCompletionHandler: (UIBackgroundFetchResult) -> Unit
    ) = scope.delegate(fetchCompletionHandler, { UIBackgroundFetchResultNoData }) {
        delegate.didReceiveRemoteNotification(
            application = application,
            userInfo = didReceiveRemoteNotification,
        )
    }

    override fun application(
        application: UIApplication,
        handleWatchKitExtensionRequest: Map<Any?, *>?,
        reply: (Map<Any?, *>?) -> Unit
    ) = scope.delegate(reply, { null }) {
        delegate.handleWatchKitExtensionRequest(
            application = application,
            userInfo = handleWatchKitExtensionRequest,
        )
    }

    override fun applicationShouldRequestHealthAuthorization(
        application: UIApplication,
    ) = delegate.shouldRequestHealthAuthorization(
        application = application,
    )

    override fun application(
        application: UIApplication,
        shouldAllowExtensionPointIdentifier: UIApplicationExtensionPointIdentifier,
    ): Boolean = delegate.shouldAllowExtensionPointIdentifier(
        application = application,
        extensionPointIdentifier = shouldAllowExtensionPointIdentifier,
    ).orTrue

    override fun application(
        application: UIApplication,
        handlerForIntent: INIntent,
    ): Any? = delegate.handlerForIntent(
        application = application,
        intent = handlerForIntent,
    ).orNull

    override fun applicationShouldAutomaticallyLocalizeKeyCommands(
        application: UIApplication,
    ): Boolean = delegate.shouldAutomaticallyLocalizeKeyCommands(
        application = application,
    ).orTrue

    override fun application(
        application: UIApplication,
        supportedInterfaceOrientationsForWindow: UIWindow?,
    ): UIInterfaceOrientationMask = delegate.supportedInterfaceOrientations(
        application = application,
        window = supportedInterfaceOrientationsForWindow,
    ).valueOr(UIInterfaceOrientationMaskAll)

    companion object : UIApplicationDelegateProtocolMeta, NSObjectMeta() {
        var delegate: ApplicationDelegateProtocol
            get() = globalDelegate
            set(value) {
                globalDelegate = value
            }
    }
}

private var globalDelegate: ApplicationDelegateProtocol = object : ApplicationDelegateProtocol {}








