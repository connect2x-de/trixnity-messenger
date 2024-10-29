package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module

fun interface MinimiseApp {
    operator fun invoke()
}

expect fun platformMinimiseAppModule(): Module
