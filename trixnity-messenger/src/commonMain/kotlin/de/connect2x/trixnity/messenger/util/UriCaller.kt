package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module

fun interface UriCaller {
    /**
     * @param external Open link in app or same Browser tab or in an external app or new Browser tab
     */
    operator fun invoke(
        uri: String, // there is no multiplatform Uri that we are aware of, so we use String
        external: Boolean,
    )
}

expect fun platformUriCallerModule(): Module
