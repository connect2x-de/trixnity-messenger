package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.util.StoragePrefix
import org.koin.core.module.Module
import org.koin.dsl.module
import web.idb.indexedDB

internal actual fun platformDeleteProfileDataModule(): Module = module {
    single<DeleteProfileData> {
        val storagePrefix = get<StoragePrefix>().storagePrefix
        DeleteProfileData { profile ->
            val databaseNamesPromise = indexedDB.databases()
            databaseNamesPromise.then { databases ->
                databases.forEach { database ->
                    database.name?.let { databaseName ->
                        if (databaseName.startsWith("$storagePrefix$profile/")) {
                            indexedDB.deleteDatabase(databaseName)
                        }
                    }
                }
            }
        }
    }
}
