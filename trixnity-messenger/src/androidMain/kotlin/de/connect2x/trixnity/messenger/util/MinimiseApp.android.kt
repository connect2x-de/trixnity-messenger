package de.connect2x.trixnity.messenger.util

import android.content.Context
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformMinimiseAppModule(): Module = module {
    single<MinimiseApp> {
        val context = get<Context>()
        MinimiseApp {
            context.findActivity()?.moveTaskToBack(true)
        }
    }
}
