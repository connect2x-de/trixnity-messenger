package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module
import web.navigator.navigator

actual fun platformIsNetworkAvailableModule(): Module = module {
    single<IsNetworkAvailable> {
        IsNetworkAvailable { navigator.onLine }
    }
}