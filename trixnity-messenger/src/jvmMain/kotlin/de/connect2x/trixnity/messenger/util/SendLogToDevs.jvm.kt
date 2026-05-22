package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.lognity.api.marker.Marker
import io.ktor.http.*
import java.awt.Desktop
import java.net.URI
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.module.Module
import org.koin.dsl.module

private val log: Logger = Logger("de.connect2x.trixnity.messenger.util.SendLogToDevsKt")

actual fun platformSendLogToDevsModule(): Module = module {
    single<SendLogToDevs> {
        val rootPath = get<RootPath>().path
        SendLogToDevs { emailAddress, subject ->
            val content =
                try {
                    withContext(Dispatchers.IO) {
                        Files.readString(rootPath.resolve("messenger.log").toNioPath()) // TODO configurable and as file
                    }
                } catch (exc: Exception) {
                    log.error(exc as Marker?) { "cannot read log content" }
                    ""
                }
            try {
                val desktop = Desktop.getDesktop()
                val mailURIStr =
                    String.format(
                        "mailto:%s?subject=%s&body=%s",
                        emailAddress,
                        subject.encodeURLParameter(),
                        content.encodeURLParameter(),
                    )
                val mailURI = URI(mailURIStr)
                desktop.mail(mailURI)
            } catch (exc: Exception) {
                log.error(exc) { "cannot open mail program to send logs" }
            }
        }
    }
}
