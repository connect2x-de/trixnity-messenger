package de.connect2x.trixnity.messenger.util

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSURL.Companion.URLWithString
import platform.UIKit.UIApplication

private val log = KotlinLogging.logger { }

actual fun platformUriCallerModule(): Module = module {
    single<UriCaller> {
        UriCaller { uri, external ->
            val safeUri = checkNotNull(URLWithString(uri))
            log.info { "call uri: $safeUri" }
            if (!external) log.debug { "does not support internal uri calling yet" }
            UIApplication.sharedApplication.openURL(safeUri)
        }
    }
}
