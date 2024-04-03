package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSURL.Companion.URLWithString
import platform.UIKit.UIApplication

actual fun platformUriCallerModule(): Module = module {
    single<UriCaller> {
        UriCaller { uri -> UIApplication.sharedApplication.openURL(checkNotNull(URLWithString(uri))) }
    }
}
