package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module
import java.awt.Desktop
import java.net.URI

actual fun platformUriCallerModule(): Module = module {
    single<UriCaller> {
        UriCaller { uri ->
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI(uri))
            } else when (getOs()) {
                OS.LINUX -> {
                    Runtime.getRuntime().exec(arrayOf("xdg-open", URI(uri).toString()))
                }

                else -> throw UnsupportedOperationException("AWT does not support the BROWSE action on this platform")

            }
        }
    }
}

