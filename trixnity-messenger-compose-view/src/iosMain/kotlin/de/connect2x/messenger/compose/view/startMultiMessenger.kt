package de.connect2x.messenger.compose.view

import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import de.connect2x.trixnity.messenger.KUIApplicationDelegateProtocol
import de.connect2x.trixnity.messenger.KUIWindowSceneDelegateProtocol
import de.connect2x.trixnity.messenger.MatrixMultiMessengerService
import de.connect2x.trixnity.messenger.UIApplicationDelegateProxy
import de.connect2x.trixnity.messenger.UIWindowSceneDelegateProxy
import de.connect2x.trixnity.messenger.activeDI
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toCValues
import kotlinx.coroutines.runBlocking
import objcnames.classes.INIntent
import objcnames.classes.INIntentResponse
import org.koin.dsl.module
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSStringFromClass
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDelegateProtocol
import platform.UIKit.UIApplicationDelegateProtocolMeta
import platform.UIKit.UIApplicationMain
import platform.UIKit.UIBackgroundFetchResult
import platform.UIKit.UILocalNotification
import platform.UIKit.UIScene
import platform.UIKit.UISceneConfiguration
import platform.UIKit.UISceneConnectionOptions
import platform.UIKit.UISceneSession
import platform.UIKit.UIUserNotificationSettings
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.UIWindowSceneDelegateProtocol
import platform.UIKit.UIWindowSceneDelegateProtocolMeta
import platform.darwin.NSObject
import platform.darwin.NSObjectMeta

// inspired from https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/mpp/demo/src/uikitMain/kotlin/androidx/compose/mpp/demo/main.uikit.kt
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun startMultiMessenger(
    args: List<String>,
    configuration: MatrixMultiMessengerConfiguration.() -> Unit,
) {
    MatrixMultiMessengerService.configuration = {
        configuration()

        modulesFactories += listOf({
            module {
                single<KUIApplicationDelegateProtocol> { MultiMessengerApplicationDelegate() }
                single<KUIWindowSceneDelegateProtocol> { MultiMessengerSceneDelegate() }
            }
        })

    }
    runBlocking {
        MatrixMultiMessengerService.init()
    }
    memScoped {
        val argc = args.size
        val argv = args.map { it.cstr.ptr }.toCValues()
        autoreleasepool {
            UIApplicationMain(argc, argv, null, NSStringFromClass(AppDelegate))
        }
    }
}

@ExportObjCClass
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
class AppDelegate : UIApplicationDelegateProtocol, NSObject {
    companion object : UIApplicationDelegateProtocolMeta, NSObjectMeta()

    @OverrideInit
    @Suppress("Unused")
    constructor() : super()

    object UIApplicationDelegate : UIApplicationDelegateProxy(activeDI.getAll<KUIApplicationDelegateProtocol>())

    override fun application(
        application: UIApplication,
        configurationForConnectingSceneSession: UISceneSession,
        options: UISceneConnectionOptions
    ): UISceneConfiguration =
        UIApplicationDelegate.application(
            application = application,
            configurationForConnectingSceneSession = configurationForConnectingSceneSession,
            options = options
        ) ?: UISceneConfiguration()

    override fun application(application: UIApplication, didFinishLaunchingWithOptions: Map<Any?, *>?): Boolean =
        UIApplicationDelegate.application(
            application = application,
            didFinishLaunchingWithOptions = didFinishLaunchingWithOptions
        )

    override fun application(
        application: UIApplication,
        handleIntent: INIntent,
        completionHandler: (INIntentResponse?) -> Unit
    ) = UIApplicationDelegate.application(
        application = application,
        handleIntent = handleIntent,
        completionHandler = completionHandler
    )

    override fun application(
        application: UIApplication,
        didReceiveLocalNotification: UILocalNotification
    ) = UIApplicationDelegate.application(
        application = application,
        didReceiveLocalNotification = didReceiveLocalNotification
    )

    override fun application(
        application: UIApplication,
        openURL: NSURL,
        sourceApplication: String?,
        annotation: Any
    ): Boolean = UIApplicationDelegate.application(
        application = application,
        openURL = openURL,
        sourceApplication = sourceApplication,
        annotation = annotation
    )

    override fun application(
        application: UIApplication,
        handleActionWithIdentifier: String?,
        forLocalNotification: UILocalNotification,
        withResponseInfo: Map<Any?, *>,
        completionHandler: () -> Unit
    ) = UIApplicationDelegate.application(
        application = application,
        handleActionWithIdentifier = handleActionWithIdentifier,
        forLocalNotification = forLocalNotification,
        withResponseInfo = withResponseInfo,
        completionHandler = completionHandler
    )

    override fun application(
        application: UIApplication,
        didRegisterUserNotificationSettings: UIUserNotificationSettings
    ) = UIApplicationDelegate.application(
        application = application,
        didRegisterUserNotificationSettings = didRegisterUserNotificationSettings
    )

    override fun application(
        application: UIApplication,
        handleOpenURL: NSURL
    ): Boolean = UIApplicationDelegate.application(
        application = application,
        handleOpenURL = handleOpenURL
    )

    override fun application(
        application: UIApplication,
        didReceiveRemoteNotification: Map<Any?, *>,
        fetchCompletionHandler: (UIBackgroundFetchResult) -> Unit
    ) = UIApplicationDelegate.application(
        application = application,
        didReceiveRemoteNotification = didReceiveRemoteNotification,
        fetchCompletionHandler = fetchCompletionHandler
    )

    override fun application(
        application: UIApplication,
        handlerForIntent: INIntent
    ): Any? = UIApplicationDelegate.application(
        application = application,
        handlerForIntent = handlerForIntent
    )

    override fun application(
        application: UIApplication,
        handleActionWithIdentifier: String?,
        forRemoteNotification: Map<Any?, *>,
        withResponseInfo: Map<Any?, *>,
        completionHandler: () -> Unit
    ) = UIApplicationDelegate.application(
        application = application,
        handleActionWithIdentifier = handleActionWithIdentifier,
        forRemoteNotification = forRemoteNotification,
        withResponseInfo = withResponseInfo,
        completionHandler = completionHandler
    )

    override fun application(
        application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken: NSData
    ) = UIApplicationDelegate.application(
        application = application,
        didRegisterForRemoteNotificationsWithDeviceToken = didRegisterForRemoteNotificationsWithDeviceToken
    )

    override fun application(
        application: UIApplication,
        handleActionWithIdentifier: String?,
        forRemoteNotification: Map<Any?, *>,
        completionHandler: () -> Unit
    ) = UIApplicationDelegate.application(
        application = application,
        handleActionWithIdentifier = handleActionWithIdentifier,
        forRemoteNotification = forRemoteNotification,
        completionHandler = completionHandler
    )

    override fun application(
        application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError: NSError
    ) = UIApplicationDelegate.application(
        application = application,
        didFailToRegisterForRemoteNotificationsWithError = didFailToRegisterForRemoteNotificationsWithError
    )

    override fun application(
        application: UIApplication,
        didReceiveRemoteNotification: Map<Any?, *>,
    ) = UIApplicationDelegate.application(
        application = application,
        didReceiveRemoteNotification = didReceiveRemoteNotification
    )

    override fun application(
        app: UIApplication,
        openURL: NSURL,
        options: Map<Any?, *>
    ): Boolean = UIApplicationDelegate.application(
        app = app,
        openURL = openURL,
        options = options
    )

    override fun application(
        application: UIApplication,
        handleActionWithIdentifier: String?,
        forLocalNotification: UILocalNotification,
        completionHandler: () -> Unit
    ) = UIApplicationDelegate.application(
        application = application,
        handleActionWithIdentifier = handleActionWithIdentifier,
        forLocalNotification = forLocalNotification,
        completionHandler = completionHandler
    )

    override fun applicationDidEnterBackground(application: UIApplication) =
        UIApplicationDelegate.applicationDidEnterBackground(application)

    override fun applicationDidReceiveMemoryWarning(application: UIApplication) =
        UIApplicationDelegate.applicationDidReceiveMemoryWarning(application)

    override fun applicationWillTerminate(application: UIApplication) =
        UIApplicationDelegate.applicationWillTerminate(application)
}

@ExportObjCClass
@OptIn(BetaInteropApi::class)
class SceneDelegate : UIWindowSceneDelegateProtocol, NSObject {

    companion object : UIWindowSceneDelegateProtocolMeta, NSObjectMeta()

    @OverrideInit
    @Suppress("Unused")
    constructor() : super()

    object UIWindowSceneDelegate : UIWindowSceneDelegateProxy(activeDI.getAll<KUIWindowSceneDelegateProtocol>())

    override fun window(): UIWindow? = UIWindowSceneDelegate.window()

    override fun setWindow(window: UIWindow?) = UIWindowSceneDelegate.setWindow(window)

    override fun scene(
        scene: UIScene,
        didFailToContinueUserActivityWithType: String,
        error: NSError
    ) = UIWindowSceneDelegate.scene(
        scene = scene,
        didFailToContinueUserActivityWithType = didFailToContinueUserActivityWithType,
        error = error
    )

    override fun scene(scene: UIScene, willContinueUserActivityWithType: String) =
        UIWindowSceneDelegate.scene(scene = scene, willContinueUserActivityWithType = willContinueUserActivityWithType)

    override fun scene(
        scene: UIScene,
        willConnectToSession: UISceneSession,
        options: UISceneConnectionOptions
    ) = UIWindowSceneDelegate.scene(scene = scene, willConnectToSession = willConnectToSession, options = options)

    override fun scene(scene: UIScene, openURLContexts: Set<*>) =
        UIWindowSceneDelegate.scene(scene = scene, openURLContexts = openURLContexts)

    override fun sceneDidBecomeActive(scene: UIScene) = UIWindowSceneDelegate.sceneDidBecomeActive(scene = scene)

    override fun sceneDidDisconnect(scene: UIScene) = UIWindowSceneDelegate.sceneDidDisconnect(scene = scene)

    override fun sceneDidEnterBackground(scene: UIScene) = UIWindowSceneDelegate.sceneDidEnterBackground(scene = scene)

    override fun sceneWillEnterForeground(scene: UIScene) =
        UIWindowSceneDelegate.sceneWillEnterForeground(scene = scene)

    override fun sceneWillResignActive(scene: UIScene) = UIWindowSceneDelegate.sceneWillResignActive(scene = scene)

}


class MultiMessengerApplicationDelegate : KUIApplicationDelegateProtocol {

    override fun application(
        application: UIApplication,
        didFinishLaunchingWithOptions: Map<Any?, *>?
    ): Boolean = true

    override fun application(
        application: UIApplication,
        configurationForConnectingSceneSession: UISceneSession,
        options: UISceneConnectionOptions
    ): UISceneConfiguration = UISceneConfiguration(
        name = null,
        sessionRole = configurationForConnectingSceneSession.role,
    ).apply {
        delegateClass = SceneDelegate
        sceneClass = UIWindowScene
    }
}

class MultiMessengerSceneDelegate : KUIWindowSceneDelegateProtocol {

    private val lifecycle by lazy { ApplicationLifecycle() }

    // @formatter:off
    private var window: UIWindow? = null
    override fun window(): UIWindow? {
        return window
    }
    override fun setWindow(window: UIWindow?) { this.window = window }
    // @formatter:on

    override fun scene(
        scene: UIScene,
        willConnectToSession: UISceneSession,
        options: UISceneConnectionOptions
    ) {
        val windowScene = scene as? UIWindowScene ?: return
        val newWindow = UIWindow(windowScene = windowScene)

        val rootViewController = MultiMessengerViewController(lifecycle)
        newWindow.rootViewController = rootViewController
        newWindow.makeKeyAndVisible()
        window = newWindow
    }
}
