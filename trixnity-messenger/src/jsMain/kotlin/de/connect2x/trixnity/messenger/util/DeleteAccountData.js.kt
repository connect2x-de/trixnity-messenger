package de.connect2x.trixnity.messenger.util

import com.juul.indexeddb.deleteDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun platformDeleteAccountDataModule(): Module = module {
    single<DeleteAccountData> {
        DeleteAccountData { userId ->
            deleteDatabase(getRepositoryDatabaseName(userId))
            deleteDatabase(getMediaDatabaseName(userId))
        }
    }
}