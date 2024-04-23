package de.connect2x.trixnity.messenger.util

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSURL.Companion.URLWithString
import platform.UIKit.UIApplication

private val log = KotlinLogging.logger { }

actual fun platformUriCallerModule(): Module = module {
    single<UriCaller> {
        UriCaller { uri ->
            log.info { "call uri: $uri" }
            UIApplication.sharedApplication.openURL(checkNotNull(URLWithString(uri)))
        }
    }
}
