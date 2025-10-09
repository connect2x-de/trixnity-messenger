package de.connect2x.trixnity.messenger.compose.app

import android.app.Application
import de.connect2x.trixnity.messenger.MatrixMultiMessengerService
import de.connect2x.trixnity.messenger.notification.fcm.addFcmPushNotificationProvider

class MessengerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MatrixMultiMessengerService.configuration = {
            configure()
            addFcmPushNotificationProvider()
            messengerConfiguration {
                pushUrl = "https://sygnal.demo.timmy-messenger.de/_matrix/push/v1/notify"
                pushAppId = "$appId.fcm"
            }
        }
    }
}
