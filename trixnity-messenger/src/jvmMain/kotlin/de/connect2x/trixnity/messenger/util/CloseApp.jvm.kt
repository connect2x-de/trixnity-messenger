package de.connect2x.trixnity.messenger.util

import kotlin.system.exitProcess
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCloseAppModule(): Module = module { single<CloseApp> { CloseApp { exitProcess(0) } } }
