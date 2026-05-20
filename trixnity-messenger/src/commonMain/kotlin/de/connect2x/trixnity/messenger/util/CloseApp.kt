package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module

// TODO we should find a better alternative instead of closing app, because it is only supported on Android and Desktop
fun interface CloseApp {
    operator fun invoke()
}

expect fun platformCloseAppModule(): Module
