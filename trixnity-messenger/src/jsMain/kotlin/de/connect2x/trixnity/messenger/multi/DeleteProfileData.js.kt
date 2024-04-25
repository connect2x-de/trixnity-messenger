package de.connect2x.trixnity.messenger.multi

import com.juul.indexeddb.deleteDatabase
import de.connect2x.trixnity.messenger.util.StoragePrefix
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun platformDeleteProfileDataModule(): Module = module {
    single<DeleteProfileData> {
        val storagePrefix = get<StoragePrefix>().storagePrefix
        DeleteProfileData { profile, userIds ->
            userIds.forEach { userId ->
                deleteDatabase(getProfileDatabaseName(storagePrefix, profile, userId))
                deleteDatabase(getProfileMediaDatabaseName(storagePrefix, profile, userId))
            }
        }
    }
}
