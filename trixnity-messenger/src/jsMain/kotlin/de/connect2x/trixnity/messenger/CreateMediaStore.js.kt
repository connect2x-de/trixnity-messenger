package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.RootPath
import net.folivo.trixnity.client.media.indexeddb.IndexedDBMediaStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateMediaStoreModule(): Module = module {
    single<CreateMediaStore> {
        val rootPath = get<RootPath>()
        CreateMediaStore { userId ->
            IndexedDBMediaStore(rootPath.forAccountDatabase(userId).toString())
        }
    }
}
