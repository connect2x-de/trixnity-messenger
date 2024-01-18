package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module

fun interface GetDefaultDeviceDisplayName {
    operator fun invoke(): String
}

expect fun platformGetDefaultDisplayNameModule(): Module
