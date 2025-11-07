package de.connect2x.trixnity.messenger.compose.app

import de.connect2x.messenger.compose.view.startMultiMessenger
import de.connect2x.trixnity.messenger.notification.apns.addApnsPushNotificationProvider

@Throws(IllegalStateException::class)
fun main(args: List<String>) {
    try {
        startMultiMessenger(args) {
            configure()
            addApnsPushNotificationProvider()
            messengerConfiguration {
                pushUrl = "https://sygnal.demo.timmy-messenger.de/_matrix/push/v1/notify"
                pushAppId = "$appId.apns"
            }
        }
    } catch(t: Throwable) {
        throw IllegalStateException(t)
    }
}
