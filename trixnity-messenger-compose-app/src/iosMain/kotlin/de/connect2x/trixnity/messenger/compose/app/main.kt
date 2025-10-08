package de.connect2x.trixnity.messenger.compose.app

import de.connect2x.trixnity.messenger.compose.view.startMultiMessenger
import de.connect2x.trixnity.messenger.notification.apns.addApnsPushNotificationProvider

fun main(args: Array<String>) {
    startMultiMessenger(args) {
        configure()
        addApnsPushNotificationProvider()
    }
}
