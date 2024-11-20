package de.connect2x.messenger.android

import android.content.ClipData
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

fun ClipData.toSequence(): Sequence<ClipData.Item> = sequence {
    for (i in 0 until itemCount) {
        yield(getItemAt(i))
    }
}

fun ClipData.toList(): List<ClipData.Item> = toSequence().toList()
