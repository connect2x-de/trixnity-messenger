package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSURL.Companion.URLWithString
import platform.UIKit.UIApplication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private val log: Logger = Logger("de.connect2x.trixnity.messenger.util.UriCallerKt")

actual fun platformUriCallerModule(): Module = module {
    single<UriCaller> {
        UriCaller { uri, external ->
            val safeUri = checkNotNull(URLWithString(uri))
            log.info { "call uri: $safeUri" }
            if (!external) log.debug { "does not support internal uri calling yet" }

            // openURL requires to be called on the main queue, because otherwise it will be called on a background
            // thread.
            dispatch_async(dispatch_get_main_queue()) {
                UIApplication.sharedApplication.openURL(safeUri, emptyMap<Any?, Any?>()) {}
            }
        }
    }
}
