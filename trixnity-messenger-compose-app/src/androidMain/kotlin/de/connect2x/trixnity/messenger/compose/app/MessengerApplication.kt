package de.connect2x.trixnity.messenger.compose.app

import android.app.Application
import de.connect2x.lognity.api.backend.Backend
import de.connect2x.lognity.backend.DefaultBackend
import de.connect2x.lognity.config.CoreConfigExtension
import de.connect2x.lognity.config.SerializableConfig
import de.connect2x.lognity.config.setDefaultConfig
import de.connect2x.trixnity.messenger.MatrixMultiMessengerService
import de.connect2x.trixnity.messenger.notification.fcm.addFcmPushNotificationProvider
import kotlinx.io.asSource
import kotlinx.io.buffered

class MessengerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Backend.set(DefaultBackend)
        SerializableConfig uses CoreConfigExtension
        checkNotNull(this::class.java.getResourceAsStream("lognity.json")).use { stream ->
            Backend.setDefaultConfig(stream.asSource().buffered())
        }
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
