package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformGetSecretStringKeyModule(): Module = module {
    single<GetSecretStringKey> {
        GetSecretStringKey { id, create ->
            // TODO !!!
            null
        }
    }
}