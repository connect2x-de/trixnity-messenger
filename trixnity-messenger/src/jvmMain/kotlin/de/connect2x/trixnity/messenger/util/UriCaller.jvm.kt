package de.connect2x.trixnity.messenger.util

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.module.Module
import org.koin.dsl.module
import java.awt.Desktop
import java.net.URI

private val log = KotlinLogging.logger { }

actual fun platformUriCallerModule(): Module = module {
    single<UriCaller> {
        UriCaller { uri, external ->
            val safeUri = URI(uri)
            log.info { "call uri: $safeUri" }
            if (!external) log.debug { "does not support internal uri calling yet" }
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(safeUri)
                } catch (exc: Exception) {
                    log.error(exc) { "cannot open uri '$safeUri'" }
                }
            } else when (getOs()) {
                OS.LINUX -> {
                    try {
                        Runtime.getRuntime().exec(arrayOf("xdg-open", safeUri.toString()))
                    } catch (exc: Exception) {
                        log.error(exc) { "cannot open uri '$safeUri'" }
                    }
                }

                else -> throw UnsupportedOperationException("AWT does not support the BROWSE action on this platform")

            }
        }
    }
}

