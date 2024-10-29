package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformMinimiseAppModule(): Module = module {
    single<MinimiseApp> {
        MinimiseApp {
            // unnecessary on Desktop atm and only possible through Compose (rememberWindowState())
        }
    }
}
