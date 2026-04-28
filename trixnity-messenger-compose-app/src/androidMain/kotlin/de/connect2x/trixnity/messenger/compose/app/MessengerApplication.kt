package de.connect2x.trixnity.messenger.compose.app

import android.app.Application
import de.connect2x.lognity.api.backend.Backend
import de.connect2x.lognity.backend.DefaultBackend
import de.connect2x.lognity.config.CoreConfigExtension
import de.connect2x.lognity.config.SerializableConfig
import de.connect2x.lognity.config.setDefaultConfig
import de.connect2x.trixnity.messenger.MatrixMultiMessengerService
import de.connect2x.trixnity.messenger.notification.fcm.addFcmPushNotificationProvider
import de.connect2x.trixnity.messenger.notification.unifiedpush.addUnifiedPushNotificationProvider
import kotlinx.io.asSource
import kotlinx.io.buffered

class MessengerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Backend.set(DefaultBackend)
        SerializableConfig uses CoreConfigExtension
        applicationContext.assets.open("lognity.json").buffered().use { stream ->
            Backend.setDefaultConfig(stream.asSource().buffered())
        }
        MatrixMultiMessengerService.configuration = {
            configure()
            addFcmPushNotificationProvider(
                pushUrl = "https://sygnal.matrix.dev.connect2x.de/_matrix/push/v1/notify",
                pushAppId = "$appId.fcm",
            )
            addUnifiedPushNotificationProvider(
                pushUrl = "https://ntfy.matrix.dev.connect2x.de/_matrix/push/v1/notify",
                pushAppId = "$appId.unifiedpush",
            )
        }
    }
}
