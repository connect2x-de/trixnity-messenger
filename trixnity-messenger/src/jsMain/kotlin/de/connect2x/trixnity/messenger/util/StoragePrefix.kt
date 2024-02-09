package de.connect2x.trixnity.messenger.util

import org.koin.dsl.module

value class StoragePrefix(
    /**
     * Usually it should end with an '/', so you get names like "prefix/@user:server/database"
     */
    val storagePrefix: String
)

fun platformStoragePrefixModule() = module {
    single<StoragePrefix> {
        StoragePrefix("")
    }
}