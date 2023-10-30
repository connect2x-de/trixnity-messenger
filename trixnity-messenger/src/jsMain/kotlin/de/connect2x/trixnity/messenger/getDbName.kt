package de.connect2x.trixnity.messenger

internal fun getDbName(accountName: String) =
    "${MessengerConfig.instance.appName.replaceFirstChar { it.lowercase() }}-$accountName"

internal fun getMediaStoreName(accountName: String) =
    getDbName(accountName) + "-media"