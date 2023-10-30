package de.connect2x.trixnity.messenger

actual fun deviceDisplayName(): String {
    return "${MessengerConfig.instance.appName.replaceFirstChar { it.lowercase() }} (Browser)"
}