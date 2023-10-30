package de.connect2x.trixnity.messenger

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger { }
actual suspend fun getLogContent(): String {
    log.warn { "get log content not supported on js yet" }
    return ""
}