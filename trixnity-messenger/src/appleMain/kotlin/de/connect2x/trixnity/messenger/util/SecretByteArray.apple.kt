package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformGetSecretByteArrayKeyModule(): Module = module {
    single<GetSecretByteArrayKey> {
        GetSecretByteArrayKey { id, create ->
            // TODO !!!
            null
        }
    }
}