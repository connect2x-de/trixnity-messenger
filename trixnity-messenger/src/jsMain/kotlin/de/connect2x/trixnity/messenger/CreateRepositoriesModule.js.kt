package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.SecretString
import de.connect2x.trixnity.messenger.util.getRepositoryDatabaseName
import net.folivo.trixnity.client.store.repository.indexeddb.createIndexedDBRepositoriesModule
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> {
        object : CreateRepositoriesModule {
            override suspend fun create(userId: UserId): CreateRepositoriesModule.CreateResult =
                CreateRepositoriesModule.CreateResult(
                    module = createInternal(userId),
                    databasePassword = null,
                )

            override suspend fun load(userId: UserId, databasePassword: SecretString?): Module =
                createInternal(userId)

            private suspend fun createInternal(userId: UserId): Module =
                createIndexedDBRepositoriesModule(getRepositoryDatabaseName(userId))
        }
    }
}