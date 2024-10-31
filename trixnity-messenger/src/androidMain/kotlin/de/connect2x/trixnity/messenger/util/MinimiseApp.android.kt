package de.connect2x.trixnity.messenger.util

import android.app.Activity
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformMinimizeAppModule(): Module = module {
    single<MinimizeApp> {
        val activity = get<Activity>()
        MinimizeApp {
            activity.moveTaskToBack(true)
        }
    }
}
