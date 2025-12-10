package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.RootPath
import net.folivo.trixnity.client.RepositoriesModule
import net.folivo.trixnity.client.store.repository.indexeddb.indexedDB
import net.folivo.trixnity.core.model.UserId
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

            private fun createInternal(userId: UserId): RepositoriesModule =
                RepositoriesModule.indexedDB(rootPath.forAccountDatabase(userId).toString())
        }
    }
}
