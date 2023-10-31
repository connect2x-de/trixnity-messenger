package de.connect2x.trixnity.messenger

actual suspend fun getLogContent(): String {
    val logFile = getContext().filesDir.resolve("timmy.log")
    return logFile.absolutePath
}