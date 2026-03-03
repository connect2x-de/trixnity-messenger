package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import org.koin.core.module.Module
import org.koin.dsl.module
import web.url.URL
import web.window.WindowTarget
import web.window._blank
import web.window._self
import web.window.window

private val log: Logger = Logger("de.connect2x.trixnity.messenger.util.UriCallerKt")

actual fun platformUriCallerModule(): Module = module {
    single<UriCaller> {
        UriCaller { uri, external ->
            val safeUri = URL(uri)
            log.info { "call uri: $safeUri" }
            val target = if (external) WindowTarget._blank else WindowTarget._self
            window.open(safeUri, target)
        }
    }
}
