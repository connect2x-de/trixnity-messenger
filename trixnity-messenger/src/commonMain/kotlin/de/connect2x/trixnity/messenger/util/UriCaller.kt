package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module

fun interface UriCaller {
    suspend operator fun invoke(uri: String) // there is no multiplatform Uri that we are aware of, so we use String
}

expect fun platformUriCallerModule(): Module
