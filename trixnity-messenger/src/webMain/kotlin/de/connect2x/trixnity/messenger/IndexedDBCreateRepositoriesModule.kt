package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.client.RepositoriesModule
import de.connect2x.trixnity.client.store.repository.indexeddb.indexedDB
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.util.RootPath

internal class IndexedDBCreateRepositoriesModule(private val rootPath: RootPath) : CreateRepositoriesModule {
    override suspend fun generateDatabaseKey(): ByteArray? {
        return null
    }

    override suspend fun create(userId: UserId, databaseKey: ByteArray?): RepositoriesModule {
        return createInternal(userId)
    }

    override suspend fun load(userId: UserId, databaseKey: ByteArray?): RepositoriesModule {
        return createInternal(userId)
    }

    override fun handleExceptions(exc: Exception) {
        // empty
    }

    private fun createInternal(userId: UserId): RepositoriesModule {
        return RepositoriesModule.indexedDB(rootPath.forAccountDatabase(userId).toString())
    }
}
