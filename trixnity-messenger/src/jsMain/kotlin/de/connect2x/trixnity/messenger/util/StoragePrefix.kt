package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import org.koin.dsl.module

/**
 * This adds a prefix to every key of stored data in the web. It is used by [MatrixMultiMessenger] to manage profiles.
 */
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