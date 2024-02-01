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

fun interface EncryptRepository {
    operator fun invoke(): Boolean
}

expect fun platformEncryptRepositoryModule(): Module

actual fun platformCreateRepositoriesModuleModule(): Module = module {
    includes(platformEncryptRepositoryModule())
    single<CreateRepositoriesModule> {
        val paths = get<Paths>()
        val convertSecretByteArray = get<ConvertSecretByteArray>()
        val encryptRepository = get<EncryptRepository>()

        object : CreateRepositoriesModule {
            override suspend fun create(userId: UserId): CreateRepositoriesModule.CreateResult =
                withContext(Dispatchers.IO) {
                    val realmEncryptionKey =
                        if (encryptRepository()) SecureRandom.nextBytes(Realm.ENCRYPTION_KEY_LENGTH)
                        else null
                    CreateRepositoriesModule.CreateResult(
                        module = createRealmRepositoriesModule {
                            directory(dbFolder(userId))
                            if (realmEncryptionKey != null) encryptionKey(realmEncryptionKey)
                        },
                        databasePassword =
                        realmEncryptionKey?.let { convertSecretByteArray(it) },
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