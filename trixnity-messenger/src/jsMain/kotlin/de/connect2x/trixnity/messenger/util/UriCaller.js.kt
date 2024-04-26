package de.connect2x.trixnity.messenger.util

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.module.Module
import org.koin.dsl.module
import web.window.WindowTarget
import web.window.window

private val log = KotlinLogging.logger { }

actual fun platformUriCallerModule(): Module = module {
    single<UriCaller> {
        UriCaller { uri, external ->
            log.info { "call uri: $uri" }
            val target = if (external) WindowTarget._blank else WindowTarget._self
            window.open(uri, target)
        }
    }
}
