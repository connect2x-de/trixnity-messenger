package de.connect2x.trixnity.messenger.util

internal fun getNotificationDisplayCount(count: Int): String? =
    when {
        count == 0 -> null
        count > 99 -> "99+"
        else -> count.toString()
    }
