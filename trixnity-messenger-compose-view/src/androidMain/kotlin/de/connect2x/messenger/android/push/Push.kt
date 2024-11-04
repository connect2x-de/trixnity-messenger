package de.connect2x.messenger.android.push

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.google.firebase.messaging.FirebaseMessaging
import de.connect2x.messenger.android.NotificationHandlerProvider
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.PushMode
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.platformNotifications
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import net.folivo.trixnity.clientserverapi.model.push.PusherData
import net.folivo.trixnity.clientserverapi.model.push.SetPushers
import net.folivo.trixnity.core.model.UserId
import org.koin.core.Koin

private val log = KotlinLogging.logger { }

internal suspend fun setPush(
    context: Context,
    matrixMultiMessenger: MatrixMultiMessenger,
    pushModes: Map<UserId, PushMode>,
    matrixMessenger: MatrixMessenger,
    scope: CoroutineScope,
) {
    if (PushMode.PUSH in pushModes.values) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token != null) {
                log.debug { "Got FCM token" }
                setMatrixPushers(matrixMessenger, token, scope)
            } else {
                log.warn { "FCM token is 'null'" }
            }
        } catch (exception: Exception) {
            log.error(exception) { "Could not get FCM token" }
        }
        log.debug { "Enabling FcmService component" }
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, FcmService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
    } else {
        log.debug { "Disabling FcmService component" }
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, FcmService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
    matrixMultiMessenger.di.get<NotificationHandlerProvider>()
        .updateHandlers(context, matrixMultiMessenger.di.get(), pushModes)
}

private fun setPushersRequest(fcmToken: String, userId: UserId, deviceId: String, di: Koin): SetPushers.Request {
    val config = di.get<MatrixMultiMessengerConfiguration>()
    return SetPushers.Request(
        appDisplayName = "${config.appName} (Android)",
        appId = config.appId,
        data = PusherData(
            url = config.pushUrl,
            format = "event_id_only",
        ),
        deviceDisplayName = "$userId ($deviceId)",
        kind = "http",
        lang = "de",
        pushkey = fcmToken,
    )
}

internal fun setMatrixPushers(
    matrixMessenger: MatrixMessenger,
    fcmToken: String,
    scope: CoroutineScope,
) {
    log.debug { "Set pushers for MatrixClients that currently have their push mode active" }
    scope.launch {
        val matrixClients = matrixMessenger.di.get<MatrixClients>().first { it.isNotEmpty() }
        val pushModes =
            matrixMessenger.di.get<MatrixMessengerSettingsHolder>().value.base.accounts.map { it.key to it.value.platformNotifications.pushMode }
                .toMap()
        matrixClients.forEach { (userId, matrixClient) ->
            val pushMode = pushModes[userId]
            if (pushMode == PushMode.PUSH) {
                log.debug { "Set new push token for account $userId" }
                matrixClient.api.push.setPushers(
                    setPushersRequest(
                        fcmToken,
                        userId,
                        matrixClient.deviceId,
                        matrixMessenger.di,
                    )
                )
                    .onSuccess {
                        log.debug { "Set pushers for $userId successfully" }
                    }
                    .onFailure {
                        log.error(it) { "Cannot set pushers for $userId" }
                    }
            } else {
                log.debug {
                    "Do not set new push token for account $userId, since it's push mode is $pushMode"
                }
            }
        }
    }
}
