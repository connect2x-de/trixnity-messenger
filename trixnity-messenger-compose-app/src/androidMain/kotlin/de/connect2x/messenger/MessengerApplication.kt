package de.connect2x.messenger

import android.app.Application
import de.connect2x.trixnity.messenger.MatrixMultiMessengerService
import de.connect2x.trixnity.messenger.notification.fcm.addFirebasePushNotificationProvider

class MessengerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MatrixMultiMessengerService.configuration = {
            configure()
            addFirebasePushNotificationProvider()
        }
    }
}
