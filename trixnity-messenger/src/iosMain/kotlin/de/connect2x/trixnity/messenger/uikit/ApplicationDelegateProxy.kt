package de.connect2x.trixnity.messenger.uikit

import kotlinx.cinterop.ExperimentalForeignApi
import objcnames.classes.INIntent
import platform.Foundation.NSCoder
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationExtensionPointIdentifier
import platform.UIKit.UIApplicationLaunchOptionsKey
import platform.UIKit.UIBackgroundFetchResult
import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UISceneConfiguration
import platform.UIKit.UISceneConnectionOptions
import platform.UIKit.UISceneSession
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

open class ApplicationDelegateProxy<T : ApplicationDelegateProtocol>(
    delegates: List<T>
) : Delegator<T>(delegates), ApplicationDelegateProtocol {

    override var window: WithDefault<UIWindow?>
        get() = delegateDefault { window }
        set(value) {
            delegate { window = value }
        }

    override fun willFinishLaunching(
        application: UIApplication,
        launchOptions: Map<UIApplicationLaunchOptionsKey, *>?,
    ): WithDefault<Boolean> = delegateDefault {
        willFinishLaunching(
            application = application,
            launchOptions = launchOptions,
        )
    }

    override fun didFinishLaunching(
        application: UIApplication,
        launchOptions: Map<UIApplicationLaunchOptionsKey, *>?,
    ): WithDefault<Boolean> = delegateDefault {
        didFinishLaunching(
            application = application,
            launchOptions = launchOptions,
        )
    }

    override fun configurationForConnecting(
        application: UIApplication,
        sceneSession: UISceneSession,
        options: UISceneConnectionOptions
    ): WithDefault<UISceneConfiguration> = delegateDefault {
        configurationForConnecting(
            application = application,
            sceneSession = sceneSession,
            options = options,
        )
    }

    override fun didDiscardSceneSessions(
        application: UIApplication,
        sceneSessions: Set<UISceneSession>,
    ) = delegate {
        didDiscardSceneSessions(
            application = application,
            sceneSessions = sceneSessions,
        )
    }

    override fun willTerminate(
        application: UIApplication,
    ) = delegate {
        willTerminate(
            application = application,
        )
    }

    override fun protectedDataDidBecomeAvailable(
        application: UIApplication,
    ) = delegate {
        protectedDataDidBecomeAvailable(
            application = application,
        )
    }

    override fun protectedDataWillBecomeUnavailable(
        application: UIApplication,
    ) = delegate {
        protectedDataWillBecomeUnavailable(
            application = application,
        )
    }

    override fun didReceiveMemoryWarning(
        application: UIApplication,
    ) = delegate {
        didReceiveMemoryWarning(
            application = application,
        )
    }

    override fun significantTimeChange(
        application: UIApplication,
    ) = delegate {
        significantTimeChange(
            application = application,
        )
    }

    override fun shouldSaveSecureApplicationState(
        application: UIApplication,
        coder: NSCoder,
    ): WithDefault<Boolean> = delegateDefault {
        shouldSaveSecureApplicationState(
            application = application,
            coder = coder,
        )
    }

    override fun shouldRestoreSecureApplicationState(
        application: UIApplication,
        coder: NSCoder,
    ): WithDefault<Boolean> = delegateDefault {
        shouldRestoreSecureApplicationState(
            application = application,
            coder = coder,
        )
    }

    override fun viewController(
        application: UIApplication,
        identifierComponents: List<String>,
        coder: NSCoder,
    ): WithDefault<UIViewController?> = delegateDefault {
        viewController(
            application = application,
            identifierComponents = identifierComponents,
            coder = coder,
        )
    }

    override fun willEncodeRestorableState(
        application: UIApplication,
        coder: NSCoder,
    ) = delegate {
        willEncodeRestorableState(
            application = application,
            coder = coder,
        )
    }

    override fun didDecodeRestorableState(
        application: UIApplication,
        coder: NSCoder,
    ) = delegate {
        didDecodeRestorableState(
            application = application,
            coder = coder,
        )
    }

    override suspend fun handleEventsForBackgroundURLSession(
        application: UIApplication,
        identifier: String,
    ) = delegateSuspend {
        handleEventsForBackgroundURLSession(
            application = application,
            identifier = identifier,
        )
    }

    override fun didRegisterForRemoteNotifications(
        application: UIApplication,
        deviceToken: NSData,
    ) = delegate {
        didRegisterForRemoteNotifications(
            application = application,
            deviceToken = deviceToken,
        )
    }

    override fun didFailToRegisterForRemoteNotifications(
        application: UIApplication,
        error: NSError,
    ) = delegate {
        didFailToRegisterForRemoteNotifications(
            application = application,
            error = error,
        )
    }

    override suspend fun didReceiveRemoteNotification(
        application: UIApplication,
        userInfo: Map<Any?, *>,
    ): WithDefault<UIBackgroundFetchResult> = delegateDefaultSuspend {
        didReceiveRemoteNotification(
            application = application,
            userInfo = userInfo,
        )
    }

    override suspend fun handleWatchKitExtensionRequest(
        application: UIApplication,
        userInfo: Map<Any?, *>?,
    ): WithDefault<Map<Any?, *>?> = delegateDefaultSuspend {
        handleWatchKitExtensionRequest(
            application = application,
            userInfo = userInfo,
        )
    }

    override fun shouldRequestHealthAuthorization(
        application: UIApplication,
    ) = delegate {
        shouldRequestHealthAuthorization(
            application = application,
        )
    }

    override fun shouldAllowExtensionPointIdentifier(
        application: UIApplication,
        extensionPointIdentifier: UIApplicationExtensionPointIdentifier,
    ): WithDefault<Boolean> = delegateDefault {
        shouldAllowExtensionPointIdentifier(
            application = application,
            extensionPointIdentifier = extensionPointIdentifier,
        )
    }

    @ExperimentalForeignApi
    override fun handlerForIntent(
        application: UIApplication,
        intent: INIntent,
    ): WithDefault<Any?> = delegateDefault {
        handlerForIntent(
            application = application,
            intent = intent,
        )
    }

    override fun shouldAutomaticallyLocalizeKeyCommands(
        application: UIApplication,
    ): WithDefault<Boolean> = delegateDefault {
        shouldAutomaticallyLocalizeKeyCommands(
            application = application,
        )
    }

    override fun supportedInterfaceOrientations(
        application: UIApplication,
        window: UIWindow?,
    ): WithDefault<UIInterfaceOrientationMask> = delegateDefault {
        supportedInterfaceOrientations(
            application = application,
            window = window,
        )
    }
}
