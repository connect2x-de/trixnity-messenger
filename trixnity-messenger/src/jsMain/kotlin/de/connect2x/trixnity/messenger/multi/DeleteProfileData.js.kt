package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.util.StoragePrefix
import js.promise.await
import org.koin.core.module.Module
import org.koin.dsl.module
import web.idb.indexedDB

internal actual fun platformDeleteProfileDataModule(): Module = module {
    single<DeleteProfileData> {
        val storagePrefix = get<StoragePrefix>().storagePrefix
        DeleteProfileData { profile ->
            val allDatabases = indexedDB.databases().await()
            allDatabases.forEach { database ->
                database.name?.let { databaseName ->
                    if (databaseName.startsWith("$storagePrefix$profile/")) {
                        indexedDB.deleteDatabase(databaseName)
                    }
                }
            }
        }
    }
}
