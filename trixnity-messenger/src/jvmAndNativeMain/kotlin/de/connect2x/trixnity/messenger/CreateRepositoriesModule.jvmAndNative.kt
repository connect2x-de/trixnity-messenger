package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.ConvertSecretByteArray
import de.connect2x.trixnity.messenger.util.Paths
import de.connect2x.trixnity.messenger.util.SecretByteArray
import de.connect2x.trixnity.messenger.util.asFilesystemSafeString
import io.realm.kotlin.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.crypto.core.SecureRandom
import org.koin.core.module.Module
import org.koin.dsl.module


actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> {
        val paths = get<Paths>()
        val convertSecretByteArray = get<ConvertSecretByteArray>()

        object : CreateRepositoriesModule {
            override suspend fun create(userId: UserId): CreateRepositoriesModule.CreateResult =
                withContext(Dispatchers.IO) {
                    val realmEncryptionKey = SecureRandom.nextBytes(Realm.ENCRYPTION_KEY_LENGTH)
                    val realmEncryptionKeyAsSecretByteArray = convertSecretByteArray(realmEncryptionKey)
                    CreateRepositoriesModule.CreateResult(
                        module = createRealmRepositoriesModule {
                            directory(dbFolder(userId))
                            if (realmEncryptionKeyAsSecretByteArray !is SecretByteArray.Unencrypted)
                                encryptionKey(realmEncryptionKey)
                        },
                        databasePassword =
                        if (realmEncryptionKeyAsSecretByteArray !is SecretByteArray.Unencrypted) realmEncryptionKeyAsSecretByteArray
                        else null,
                    )
                }

            override suspend fun load(
                userId: UserId,
                databasePassword: SecretByteArray?,
            ): Module =
                withContext(Dispatchers.IO) {
                    val rawDatabasePassword = databasePassword?.let { convertSecretByteArray(it) }
                    createRealmRepositoriesModule {
                        directory(dbFolder(userId))
                        if (rawDatabasePassword != null)
                            encryptionKey(rawDatabasePassword)
                    }
                }

            private fun dbFolder(userId: UserId) =
                paths.rootPath.resolve(userId.asFilesystemSafeString()).resolve("database").toString()

        }
    }
}