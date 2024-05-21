package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformIsNetworkAvailableModule(): Module = module {
    single<IsNetworkAvailable> {
        IsNetworkAvailable {
            // TODO
            true
        }
    }
}