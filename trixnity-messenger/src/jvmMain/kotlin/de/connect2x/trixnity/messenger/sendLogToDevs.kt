package de.connect2x.trixnity.messenger

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import java.awt.Desktop
import java.net.URI

private val log = KotlinLogging.logger { }
actual fun sendLogToDevs(emailAddress: String, subject: String, content: String) {
    try {
        val desktop = Desktop.getDesktop()
        val mailURIStr = String.format(
            "mailto:%s?subject=%s&body=%s",
            emailAddress,
            subject.encodeURLParameter(),
            content.encodeURLParameter()
        )
        val mailURI = URI(mailURIStr);
        desktop.mail(mailURI)
    } catch (exc: Exception) {
        log.error(exc) { "cannot open mail program to send logs" }
    }
}