package de.connect2x.trixnity.messenger.compose.app

import android.app.Application
import de.connect2x.trixnity.messenger.MatrixMultiMessengerService
import de.connect2x.trixnity.messenger.notification.fcm.addFirebasePushNotificationProvider

class MessengerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MatrixMultiMessengerService.Companion.configuration = {
            configure()
            addFirebasePushNotificationProvider()
        }
    }
}
