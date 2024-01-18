package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.system.exitProcess

actual fun platformCloseAppModule(): Module = module {
    single<CloseApp> {
        CloseApp { exitProcess(0) }
    }
}