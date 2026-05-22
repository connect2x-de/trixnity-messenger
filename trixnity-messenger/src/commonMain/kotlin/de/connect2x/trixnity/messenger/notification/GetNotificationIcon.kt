package de.connect2x.trixnity.messenger.notification

import de.connect2x.sysnotify.NotificationIcon

// TODO this should be implemented here (e.g. using imagemagick) instead of view level
interface GetNotificationIcon {
    fun fromBytes(encoded: ByteArray, maxWidth: Int, maxHeight: Int): NotificationIcon?

    fun fromResource(path: String): NotificationIcon?
}
