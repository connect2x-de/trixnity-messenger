package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.getMediaDatabaseName
import net.folivo.trixnity.client.media.indexeddb.IndexedDBMediaStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateMediaStoreModule(): Module = module {
    single<CreateMediaStore> {
        CreateMediaStore { userId ->
            IndexedDBMediaStore(getMediaDatabaseName(userId))
        }
    }
}