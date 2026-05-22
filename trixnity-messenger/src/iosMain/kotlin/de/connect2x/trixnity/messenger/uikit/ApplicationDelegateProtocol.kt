package de.connect2x.trixnity.messenger.uikit

import de.connect2x.trixnity.messenger.uikit.WithDefault.Default
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

/** [Apple Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate) */
interface ApplicationDelegateProtocol {

    /** [Apple Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/window) */
    var window: WithDefault<UIWindow?>
        get() = Default
        set(value) {}

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:willfinishlaunchingwithoptions:))
     */
    fun willFinishLaunching(
        application: UIApplication,
        launchOptions: Map<UIApplicationLaunchOptionsKey, *>? = null,
    ): WithDefault<Boolean> = Default

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:didfinishlaunchingwithoptions:))
     */
    fun didFinishLaunching(
        application: UIApplication,
        launchOptions: Map<UIApplicationLaunchOptionsKey, *>? = null,
    ): WithDefault<Boolean> = Default

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:configurationforconnecting:options:))
     */
    fun configurationForConnecting(
        application: UIApplication,
        sceneSession: UISceneSession,
        options: UISceneConnectionOptions,
    ): WithDefault<UISceneConfiguration> = Default

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:diddiscardscenesessions:))
     */
    fun didDiscardSceneSessions(application: UIApplication, sceneSessions: Set<UISceneSession>) {}

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/applicationwillterminate(_:))
     */
    fun willTerminate(application: UIApplication) {}

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/applicationprotecteddatadidbecomeavailable(_:))
     */
    fun protectedDataDidBecomeAvailable(application: UIApplication) {}

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/applicationprotecteddatawillbecomeunavailable(_:))
     */
    fun protectedDataWillBecomeUnavailable(application: UIApplication) {}

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/applicationdidreceivememorywarning(_:))
     */
    fun didReceiveMemoryWarning(application: UIApplication) {}

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/applicationsignificanttimechange(_:))
     */
    fun significantTimeChange(application: UIApplication) {}

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:shouldsavesecureapplicationstate:))
     */
    fun shouldSaveSecureApplicationState(application: UIApplication, coder: NSCoder): WithDefault<Boolean> = Default

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:shouldrestoresecureapplicationstate:))
     */
    fun shouldRestoreSecureApplicationState(application: UIApplication, coder: NSCoder): WithDefault<Boolean> = Default

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:viewcontrollerwithrestorationidentifierpath:coder:))
     */
    fun viewController(
        application: UIApplication,
        identifierComponents: List<String>,
        coder: NSCoder,
    ): WithDefault<UIViewController?> = Default

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:willencoderestorablestatewith:))
     */
    fun willEncodeRestorableState(application: UIApplication, coder: NSCoder) {}

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:diddecoderestorablestatewith:))
     */
    fun didDecodeRestorableState(application: UIApplication, coder: NSCoder) {}

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:handleeventsforbackgroundurlsession:completionhandler:))
     */
    suspend fun handleEventsForBackgroundURLSession(application: UIApplication, identifier: String) {}

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:didregisterforremotenotificationswithdevicetoken:))
     */
    fun didRegisterForRemoteNotifications(application: UIApplication, deviceToken: NSData) {}

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:didfailtoregisterforremotenotificationswitherror:))
     */
    fun didFailToRegisterForRemoteNotifications(application: UIApplication, error: NSError) {}

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:didreceiveremotenotification:fetchcompletionhandler:))
     */
    suspend fun didReceiveRemoteNotification(
        application: UIApplication,
        userInfo: Map<Any?, *>,
    ): WithDefault<UIBackgroundFetchResult> = Default

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:handlewatchkitextensionrequest:reply:))
     */
    suspend fun handleWatchKitExtensionRequest(
        application: UIApplication,
        userInfo: Map<Any?, *>?,
    ): WithDefault<Map<Any?, *>?> = Default

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/applicationshouldrequesthealthauthorization(_:))
     */
    fun shouldRequestHealthAuthorization(application: UIApplication) {}

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:shouldallowextensionpointidentifier:))
     */
    fun shouldAllowExtensionPointIdentifier(
        application: UIApplication,
        extensionPointIdentifier: UIApplicationExtensionPointIdentifier,
    ): WithDefault<Boolean> = Default

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:handlerfor:))
     */
    @ExperimentalForeignApi
    fun handlerForIntent(application: UIApplication, intent: INIntent): WithDefault<Any?> = Default

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/applicationshouldautomaticallylocalizekeycommands(_:))
     */
    fun shouldAutomaticallyLocalizeKeyCommands(application: UIApplication): WithDefault<Boolean> = Default

    /**
     * [Apple
     * Documentation](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/application(_:supportedinterfaceorientationsfor:))
     */
    fun supportedInterfaceOrientations(
        application: UIApplication,
        window: UIWindow?,
    ): WithDefault<UIInterfaceOrientationMask> = Default
}
