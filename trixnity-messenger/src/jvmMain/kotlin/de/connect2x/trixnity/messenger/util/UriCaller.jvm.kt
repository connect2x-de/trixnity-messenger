package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import org.koin.core.module.Module
import org.koin.dsl.module
import java.awt.Desktop
import java.net.URI

private val log: Logger = Logger("de.connect2x.trixnity.messenger.util.UriCallerKt")

actual fun platformUriCallerModule(): Module = module {
    single<UriCaller> {
        UriCaller { uri, external ->
            try {
                val safeUri = URI(uri)
                log.info { "call uri: $safeUri" }
                if (!external) log.debug { "does not support internal uri calling yet" }
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(safeUri)
                } else when (getOs()) {
                    OS.LINUX -> {
                        Runtime.getRuntime().exec(arrayOf("xdg-open", safeUri.toString()))
                    }

                    else -> throw UnsupportedOperationException("AWT does not support the BROWSE action on this platform")

                }
            } catch (exc: Exception) {
                log.error(exc) { "cannot open uri '$uri'" }
            }
        }
    }
}

