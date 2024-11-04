package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module

fun interface MinimizeApp {
    operator fun invoke()
}

expect fun platformMinimizeAppModule(): Module
