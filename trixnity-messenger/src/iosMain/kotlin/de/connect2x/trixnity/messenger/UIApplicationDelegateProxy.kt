package de.connect2x.trixnity.messenger

import kotlinx.cinterop.ExperimentalForeignApi
import objcnames.classes.INIntent
import objcnames.classes.INIntentResponse
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDelegateProtocol
import platform.UIKit.UIBackgroundFetchResult
import platform.UIKit.UILocalNotification
import platform.UIKit.UIUserNotificationSettings
import platform.darwin.NSObject

/**
 * This allows to proxy calls to delegates. Not all methods are implemented. If you need more, add it.
 */
@OptIn(ExperimentalForeignApi::class)
abstract class UIApplicationDelegateProxy(
    private val delegates: List<UIApplicationDelegateProtocol>
) : UIApplicationDelegateProtocol, NSObject() {
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
        return delegates.map { it.application(application, openURL, sourceApplication, annotation) }.any { it }
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
