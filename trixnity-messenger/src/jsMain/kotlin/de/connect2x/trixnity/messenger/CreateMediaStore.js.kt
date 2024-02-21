package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.StoragePrefix
import de.connect2x.trixnity.messenger.util.getRepositoryDatabaseName
import net.folivo.trixnity.client.media.indexeddb.IndexedDBMediaStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateMediaStoreModule(): Module = module {
    single<CreateMediaStore> {
        val storagePrefix = get<StoragePrefix>().storagePrefix
        CreateMediaStore { userId ->
            IndexedDBMediaStore(getRepositoryDatabaseName(storagePrefix, userId))
        }
    }
}