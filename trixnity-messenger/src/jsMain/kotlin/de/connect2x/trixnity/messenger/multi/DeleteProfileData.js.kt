package de.connect2x.trixnity.messenger.multi

import com.juul.indexeddb.deleteDatabase
import de.connect2x.trixnity.messenger.util.StoragePrefix
import de.connect2x.trixnity.messenger.util.getMediaDatabaseName
import de.connect2x.trixnity.messenger.util.getRepositoryDatabaseName
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun platformDeleteProfileDataModule(): Module = module {
    single<DeleteProfileData> {
        val storagePrefix = get<StoragePrefix>().storagePrefix
        DeleteProfileData { profile, userId ->
            deleteDatabase(getRepositoryDatabaseName(storagePrefix, userId))
            deleteDatabase(getMediaDatabaseName(storagePrefix, userId))
        }
    }
}
