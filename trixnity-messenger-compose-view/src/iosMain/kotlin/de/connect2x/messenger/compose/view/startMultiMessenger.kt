package de.connect2x.messenger.compose.view

import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import de.connect2x.trixnity.messenger.MatrixMultiMessengerService
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toCValues
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSStringFromClass
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDelegateProtocolMeta
import platform.UIKit.UIApplicationMain
import platform.UIKit.UIScene
import platform.UIKit.UISceneConfiguration
import platform.UIKit.UISceneConnectionOptions
import platform.UIKit.UISceneSession
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.UIWindowSceneDelegateProtocolMeta
import platform.darwin.NSObjectMeta

// inspired from https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/mpp/demo/src/uikitMain/kotlin/androidx/compose/mpp/demo/main.uikit.kt
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun startMultiMessenger(
    args: Array<String>,
    configuration: MatrixMultiMessengerConfiguration.() -> Unit,
) {
    MatrixMultiMessengerService.configuration = configuration
    runBlocking {
        MatrixMultiMessengerService.init()
    }
    memScoped {
        val argc = args.size
        val argv = args.map { it.cstr.ptr }.toCValues()
        autoreleasepool {
            UIApplicationMain(argc, argv, null, NSStringFromClass(IOSAppDelegate))
        }
    }
}

private class IOSAppDelegate : MatrixMultiMessengerService.UIApplicationDelegate {
    @OptIn(BetaInteropApi::class)
    companion object Companion : UIApplicationDelegateProtocolMeta, NSObjectMeta()

    @Suppress("unused")
    @OptIn(BetaInteropApi::class)
    @OverrideInit
    constructor() : super()

    override fun application(
        application: UIApplication,
        didFinishLaunchingWithOptions: Map<Any?, *>?
    ): Boolean = true

    @OptIn(BetaInteropApi::class)
    override fun application(
        application: UIApplication,
        configurationForConnectingSceneSession: UISceneSession,
        options: UISceneConnectionOptions
    ): UISceneConfiguration {
        val config = UISceneConfiguration()
        config.delegateClass = IOSSceneDelegate.`class`()
        config.sceneClass = UIWindowScene.`class`()
        return config
    }
}

private class IOSSceneDelegate : MatrixMultiMessengerService.UIWindowSceneDelegate {
    companion object Companion : UIWindowSceneDelegateProtocolMeta, NSObjectMeta()

    private val lifecycle by lazy { ApplicationLifecycle() }

    @Suppress("unused")
    @OptIn(BetaInteropApi::class)
    @OverrideInit
    constructor() : super()

    private var _window: UIWindow? = null
    override fun window() = _window

    override fun scene(
        scene: UIScene,
        willConnectToSession: UISceneSession,
        options: UISceneConnectionOptions
    ) {
        scene as UIWindowScene
        _window = UIWindow(windowScene = scene)
        _window!!.rootViewController = MultiMessengerViewController(lifecycle)
        _window!!.makeKeyAndVisible()
    }
}
