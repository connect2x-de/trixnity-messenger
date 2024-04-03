package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module
import web.window.window

actual fun platformUriCallerModule(): Module = module {
    single<UriCaller> {
        UriCaller { uri -> window.open(uri) }
    }
}
