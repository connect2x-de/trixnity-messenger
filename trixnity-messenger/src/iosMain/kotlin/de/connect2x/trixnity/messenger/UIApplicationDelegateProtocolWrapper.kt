package de.connect2x.trixnity.messenger

import kotlinx.cinterop.ExperimentalForeignApi
import objcnames.classes.INIntent
import objcnames.classes.INIntentResponse
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundFetchResult
import platform.UIKit.UILocalNotification
import platform.UIKit.UISceneConfiguration
import platform.UIKit.UISceneConnectionOptions
import platform.UIKit.UISceneSession
import platform.UIKit.UIUserNotificationSettings

@OptIn(ExperimentalForeignApi::class)
interface KUIApplicationDelegateProtocol {
    fun application(application: UIApplication, didFinishLaunchingWithOptions: Map<Any?, *>?): Boolean = true

    fun application(
        application: UIApplication,
        configurationForConnectingSceneSession: UISceneSession,
        options: UISceneConnectionOptions
    ): UISceneConfiguration? = null

    fun application(
        application: UIApplication,
        handleIntent: INIntent,
        completionHandler: (INIntentResponse?) -> Unit
    ) = Unit

    fun application(
        application: UIApplication,
        didReceiveLocalNotification: UILocalNotification
    ) = Unit

    fun application(
        application: UIApplication,
        openURL: NSURL,
        sourceApplication: String?,
        annotation: Any
    ): Boolean = false

    fun application(
        application: UIApplication,
        handleActionWithIdentifier: String?,
        forLocalNotification: UILocalNotification,
        withResponseInfo: Map<Any?, *>,
        completionHandler: () -> Unit
    ) = Unit

    fun application(
        application: UIApplication,
        didRegisterUserNotificationSettings: UIUserNotificationSettings
    ) = Unit

    fun application(
        application: UIApplication,
        handleOpenURL: NSURL
    ): Boolean = false

    fun application(
        application: UIApplication,
        didReceiveRemoteNotification: Map<Any?, *>,
        fetchCompletionHandler: (UIBackgroundFetchResult) -> Unit
    ) = Unit

    fun application(
        application: UIApplication,
        handlerForIntent: INIntent
    ): Any? = null

    fun application(
        application: UIApplication,
        handleActionWithIdentifier: String?,
        forRemoteNotification: Map<Any?, *>,
        withResponseInfo: Map<Any?, *>,
        completionHandler: () -> Unit
    ) = Unit

    fun application(
        application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken: NSData
    ) = Unit

    fun application(
        application: UIApplication,
        handleActionWithIdentifier: String?,
        forRemoteNotification: Map<Any?, *>,
        completionHandler: () -> Unit
    ) = Unit

    fun application(
        application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError: NSError
    ) = Unit

    fun application(
        application: UIApplication,
        didReceiveRemoteNotification: Map<Any?, *>
    ) = Unit

    fun application(
        app: UIApplication,
        openURL: NSURL,
        options: Map<Any?, *>
    ): Boolean = false

    fun application(
        application: UIApplication,
        handleActionWithIdentifier: String?,
        forLocalNotification: UILocalNotification,
        completionHandler: () -> Unit
    ) = Unit

    fun applicationDidEnterBackground(application: UIApplication) = Unit

    fun applicationDidReceiveMemoryWarning(application: UIApplication) = Unit

    fun applicationWillTerminate(application: UIApplication) = Unit
}

/**
 * This allows to proxy calls to delegates. Not all methods are implemented. If you need more, add it.
 */
@OptIn(ExperimentalForeignApi::class)
abstract class UIApplicationDelegateProxy(
    private val delegates: List<KUIApplicationDelegateProtocol>
) : KUIApplicationDelegateProtocol {

    override fun application(
        application: UIApplication,
        configurationForConnectingSceneSession: UISceneSession,
        options: UISceneConnectionOptions
    ): UISceneConfiguration? {
        return delegates.firstNotNullOfOrNull {
            it.application(application = application, configurationForConnectingSceneSession = configurationForConnectingSceneSession, options = options)
        }
    }

    override fun application(
        application: UIApplication,
        handleIntent: INIntent,
        completionHandler: (INIntentResponse?) -> Unit
    ) {
        delegates.forEach { it.application(application, handleIntent, completionHandler) }
    }

    override fun application(
        application: UIApplication,
        didReceiveLocalNotification: UILocalNotification
    ) {
        delegates.forEach { it.application(application, didReceiveLocalNotification) }
    }

    override fun application(
        application: UIApplication,
        openURL: NSURL,
        sourceApplication: String?,
        annotation: Any
    ): Boolean {
        return delegates.any { it.application(application, openURL, sourceApplication, annotation) }
    }

    override fun application(
        application: UIApplication,
        handleActionWithIdentifier: String?,
        forLocalNotification: UILocalNotification,
        withResponseInfo: Map<Any?, *>,
        completionHandler: () -> Unit
    ) {
        delegates.forEach {
            it.application(
                application,
                handleActionWithIdentifier,
                forLocalNotification,
                withResponseInfo,
                completionHandler
            )
        }
    }

    override fun application(
        application: UIApplication,
        didRegisterUserNotificationSettings: UIUserNotificationSettings
    ) {
        delegates.forEach { it.application(application, didRegisterUserNotificationSettings) }
    }

    override fun application(
        application: UIApplication,
        handleOpenURL: NSURL
    ): Boolean {
        return delegates.map { it.application(application, handleOpenURL) }.any { it }
    }

    override fun application(
        application: UIApplication,
        didReceiveRemoteNotification: Map<Any?, *>,
        fetchCompletionHandler: (UIBackgroundFetchResult) -> Unit
    ) {
        delegates.forEach { it.application(application, didReceiveRemoteNotification, fetchCompletionHandler) }
    }

    override fun application(
        application: UIApplication,
        handlerForIntent: INIntent
    ): Any? {
        return delegates.firstNotNullOfOrNull { it.application(application, handlerForIntent) }
    }

    override fun application(
        application: UIApplication,
        handleActionWithIdentifier: String?,
        forRemoteNotification: Map<Any?, *>,
        withResponseInfo: Map<Any?, *>,
        completionHandler: () -> Unit
    ) {
        delegates.forEach {
            it.application(
                application,
                handleActionWithIdentifier,
                forRemoteNotification,
                withResponseInfo,
                completionHandler
            )
        }
    }

    override fun application(
        application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken: NSData
    ) {
        delegates.forEach { it.application(application, didRegisterForRemoteNotificationsWithDeviceToken) }
    }

    override fun application(
        application: UIApplication,
        handleActionWithIdentifier: String?,
        forRemoteNotification: Map<Any?, *>,
        completionHandler: () -> Unit
    ) {
        delegates.forEach {
            it.application(
                application,
                handleActionWithIdentifier,
                forRemoteNotification,
                completionHandler
            )
        }
    }

    override fun application(
        application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError: NSError
    ) {
        delegates.forEach { it.application(application, didFailToRegisterForRemoteNotificationsWithError) }
    }

    override fun application(
        application: UIApplication,
        didReceiveRemoteNotification: Map<Any?, *>
    ) {
        delegates.forEach { it.application(application, didReceiveRemoteNotification) }
    }

    override fun application(
        app: UIApplication,
        openURL: NSURL,
        options: Map<Any?, *>
    ): Boolean {
        return delegates.map { it.application(app, openURL, options) }.any { it }
    }

    override fun application(
        application: UIApplication,
        handleActionWithIdentifier: String?,
        forLocalNotification: UILocalNotification,
        completionHandler: () -> Unit
    ) {
        delegates.forEach {
            it.application(
                application,
                handleActionWithIdentifier,
                forLocalNotification,
                completionHandler
            )
        }
    }

    override fun applicationDidEnterBackground(application: UIApplication) {
        delegates.forEach { it.applicationDidEnterBackground(application) }
    }

    override fun applicationDidReceiveMemoryWarning(application: UIApplication) {
        delegates.forEach { it.applicationDidReceiveMemoryWarning(application) }
    }

    override fun applicationWillTerminate(application: UIApplication) {
        delegates.forEach { it.applicationWillTerminate(application) }
    }
}
