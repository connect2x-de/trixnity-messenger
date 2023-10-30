package de.connect2x.trixnity.messenger

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files

private val log = KotlinLogging.logger { }
actual suspend fun getLogContent(): String {
    return try {
        withContext(Dispatchers.IO) {
            Files.readString(getAppFolder(accountName = null).resolve("timmy.log"))
        }
    } catch (exc: Exception) {
        log.error(exc) { "cannot read log content" }
        ""
    }
}