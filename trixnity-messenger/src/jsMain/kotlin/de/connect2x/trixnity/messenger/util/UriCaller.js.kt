package de.connect2x.trixnity.messenger.util

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.module.Module
import org.koin.dsl.module
import web.url.URL
import web.window.WindowTarget
import web.window.window

private val log = KotlinLogging.logger { }

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
