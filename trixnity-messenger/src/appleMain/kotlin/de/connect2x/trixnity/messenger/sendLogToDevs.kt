package de.connect2x.trixnity.messenger

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger { }
actual fun sendLogToDevs(emailAddress: String, subject: String, content: String) {
    log.warn { "send log to devs is not supported on apple yet" }
// TODO
}