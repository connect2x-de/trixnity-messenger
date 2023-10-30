package de.connect2x.trixnity.messenger

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger { }
actual fun isNetworkAvailable(): Boolean {
    log.warn { "check if network is available is not supported on apple yet" }
    // TODO
    return true
}