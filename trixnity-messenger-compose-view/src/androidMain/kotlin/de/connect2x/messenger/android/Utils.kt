package de.connect2x.messenger.android

import androidx.work.ForegroundInfo
import de.connect2x.sysnotify.NotificationHandle
import de.connect2x.sysnotify.nativeNotification
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import net.folivo.trixnity.core.model.UserId

internal fun NotificationHandle.toForegroundInfo(): ForegroundInfo {
    return ForegroundInfo(hashCode(), nativeNotification)
}

internal fun pushChannelId(userId: UserId, config: MatrixMultiMessengerConfiguration) =
    "${config.appId}.push.${
        userId.full.replace("[@.]".toRegex(), "_")
    }"
