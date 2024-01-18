package de.connect2x.trixnity.messenger.util

import android.content.Context
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCloseAppModule(): Module = module {
    single<CloseApp> {
        val context = get<Context>()
        CloseApp {
            context.findActivity()?.finishAndRemoveTask()
        }
    }
}