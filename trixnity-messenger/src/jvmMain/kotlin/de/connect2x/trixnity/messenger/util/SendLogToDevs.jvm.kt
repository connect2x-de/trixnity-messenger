package de.connect2x.trixnity.messenger.util

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.awt.Desktop
import java.net.URI
import java.nio.file.Files

private val log = KotlinLogging.logger {}
actual fun platformSendLogToDevsModule(): Module = module {
    single<SendLogToDevs> {
        val paths = get<Paths>()
        SendLogToDevs { emailAddress, subject ->
            val content = try {
                withContext(Dispatchers.IO) {
                    Files.readString(paths.rootPath.resolve("timmy.log").toNioPath()) // TODO configurable and as file
                }
            } catch (exc: Exception) {
                log.error(exc) { "cannot read log content" }
                ""
            }
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
    }
}