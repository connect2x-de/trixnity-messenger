package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformMinimizeAppModule(): Module = module {
    single<MinimizeApp> {
        val activityGetter = get<ActivityGetter>()
        MinimizeApp {
            activityGetter()?.moveTaskToBack(true)
        }
    }
}
