package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.RootPath
import net.folivo.trixnity.client.store.repository.indexeddb.createIndexedDBRepositoriesModule
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> {
        val rootPath = get<RootPath>()
        object : CreateRepositoriesModule {
            override suspend fun generateDatabaseKey(): ByteArray =
                throw IllegalStateException("cannot encrypt database on web")

            override suspend fun create(userId: UserId, databaseKey: ByteArray?): Module = createInternal(userId)
            override suspend fun load(userId: UserId, databaseKey: ByteArray?): Module = createInternal(userId)

            private suspend fun createInternal(userId: UserId): Module =
                createIndexedDBRepositoriesModule(rootPath.forAccountDatabase(userId).toString())
        }
    }
}
