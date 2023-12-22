package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module

fun interface IsNetworkAvailable {
    operator fun invoke(): Boolean
}

expect fun platformIsNetworkAvailableModule(): Module
