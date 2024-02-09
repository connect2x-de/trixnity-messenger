package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.SecretByteArray
import de.connect2x.trixnity.messenger.util.StoragePrefix
import net.folivo.trixnity.client.store.repository.indexeddb.createIndexedDBRepositoriesModule
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> {
        val storagePrefix = get<StoragePrefix>().storagePrefix
        object : CreateRepositoriesModule {
            override suspend fun create(userId: UserId): CreateRepositoriesModule.CreateResult =
                CreateRepositoriesModule.CreateResult(
                    module = createInternal(userId),
                    databasePassword = null,
                )

            override suspend fun load(userId: UserId, databasePassword: SecretByteArray?): Module =
                createInternal(userId)

            private suspend fun createInternal(userId: UserId): Module =
                createIndexedDBRepositoriesModule("$storagePrefix$userId/media")
        }
    }
}