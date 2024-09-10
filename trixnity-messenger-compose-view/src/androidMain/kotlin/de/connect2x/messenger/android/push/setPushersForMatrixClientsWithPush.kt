package de.connect2x.messenger.android.push

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.PushMode
import de.connect2x.trixnity.messenger.platformNotifications
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.clientserverapi.model.push.PusherData
import net.folivo.trixnity.clientserverapi.model.push.SetPushers
import net.folivo.trixnity.core.model.UserId
import org.koin.core.Koin

private val log = KotlinLogging.logger { }

// TODO should also remove pushers:
//  If kind is not null, the pusher with this app_id and pushkey for this user is updated, or it is created if it doesn’t exist.
//  If kind is null, the pusher with this app_id and pushkey for this user is deleted.
suspend fun setPushersForMatrixClientsWithPush(
    matrixMessenger: MatrixMessenger,
    fcmToken: String,
) {
    log.debug { "set pushers for MatrixClients that currently have their push mode active" }
    val matrixClients = matrixMessenger.di.get<MatrixClients>().value
    val pushModes =
        matrixMessenger.di.get<MatrixMessengerSettingsHolder>().value.base.accounts.map { it.key to it.value.platformNotifications.pushMode }
            .toMap()
    matrixClients.forEach { (userId, matrixClient) ->
        val pushMode = pushModes[userId]
        if (pushMode == PushMode.PUSH) {
            log.debug { "set new push token for account $userId" }
            matrixClient.api.push.setPushers(setPushersRequest(matrixMessenger.di, fcmToken, userId, matrixClient.deviceId))
                .onSuccess {
                    log.debug { "set pushers for $userId successfully" }
                }
                .onFailure {
                    log.error(it) { "cannot set pushers for $userId" }
                }
        } else {
            log.debug {
                "do not set new push token for account $userId, since it's push mode is $pushMode"
            }
        }
    }
}

private fun setPushersRequest(di: Koin, fcmToken: String, userId: UserId, deviceId: String): SetPushers.Request {
    val config = di.get<MatrixMessengerConfiguration>()
    return SetPushers.Request(
        appDisplayName = "${config.appName} (Android)",
        appId = "${config.packageName}.${config.appName}.android",
        data = PusherData(
            url = "https://sygnal.demo.timmy-messenger.de/_matrix/push/v1/notify", // TODO use our push server
            format = "event_id_only",
        ),
        deviceDisplayName = "$userId ($deviceId)",
        kind = "http",
        lang = "de",
        pushkey = fcmToken,
    )
}
