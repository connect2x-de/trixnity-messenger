package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.client.RepositoriesModule
import de.connect2x.trixnity.client.store.repository.indexeddb.indexedDB
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.util.RootPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> {
        val rootPath = get<RootPath>()
        object : CreateRepositoriesModule {
            override suspend fun generateDatabaseKey(): ByteArray? = null

            override suspend fun create(userId: UserId, databaseKey: ByteArray?): RepositoriesModule =
                createInternal(userId)

            override suspend fun load(userId: UserId, databaseKey: ByteArray?): RepositoriesModule =
                createInternal(userId)

            override fun handleExceptions(exc: Exception) {
                // empty
            }

            private fun createInternal(userId: UserId): RepositoriesModule =
                RepositoriesModule.indexedDB(rootPath.forAccountDatabase(userId).toString())
        }
    }
}
