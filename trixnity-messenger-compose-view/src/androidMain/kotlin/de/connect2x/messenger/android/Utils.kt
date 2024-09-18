package de.connect2x.messenger.android

import androidx.work.ForegroundInfo
import de.connect2x.sysnotify.NotificationHandle
import de.connect2x.sysnotify.create
import de.connect2x.sysnotify.nativeNotification
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId

internal fun NotificationHandle.toForegroundInfo(): ForegroundInfo {
    return ForegroundInfo(hashCode(), nativeNotification)
}

internal fun RoomId.toNotificationHandle(): NotificationHandle {
    return NotificationHandle.create(full)
}

internal fun pushChannelId(userId: UserId, config: MatrixMultiMessengerConfiguration) =
    "${config.packageName}.push.${
        userId.full.replace("[@.]".toRegex(), "_")
    }"
