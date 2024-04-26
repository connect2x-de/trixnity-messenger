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
            log.info { "call uri: $uri" }
            if (!external) log.debug { "does not support internal uri calling yet" }
            UIApplication.sharedApplication.openURL(checkNotNull(URLWithString(uri)))
        }
    }
}
