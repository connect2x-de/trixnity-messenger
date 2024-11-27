package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCloseAppModule(): Module = module {
    single<CloseApp> {
        val activityGetter = get<ActivityGetter>()
        CloseApp {
            activityGetter()?.finishAndRemoveTask()
        }
    }
}
