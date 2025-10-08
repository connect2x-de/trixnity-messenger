package de.connect2x.messenger

import de.connect2x.messenger.compose.view.startMultiMessenger
import de.connect2x.trixnity.messenger.notification.apns.addApnsPushNotificationProvider

fun main(args: Array<String>) {
    startMultiMessenger(args) {
        configure()
        addApnsPushNotificationProvider()
    }
}
