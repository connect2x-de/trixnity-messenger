package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.ConvertSecretString
import de.connect2x.trixnity.messenger.util.Paths
import de.connect2x.trixnity.messenger.util.SecretString
import de.connect2x.trixnity.messenger.util.asFilesystemSafeString
import io.realm.kotlin.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.crypto.core.SecureRandom
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import org.koin.core.module.Module
import org.koin.dsl.module


actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> {
        val paths = get<Paths>()
        val convertSecretString = get<ConvertSecretString>()

        object : CreateRepositoriesModule {
            override suspend fun create(userId: UserId): CreateRepositoriesModule.CreateResult =
                withContext(Dispatchers.IO) {
                    val realmEncryptionKey = SecureRandom.nextBytes(Realm.ENCRYPTION_KEY_LENGTH)
                    val realmEncryptionKeyAsSecretString =
                        convertSecretString(realmEncryptionKey.toByteString().hex())
                    CreateRepositoriesModule.CreateResult(
                        module = createRealmRepositoriesModule {
                            directory(dbFolder(userId))
                            if (realmEncryptionKeyAsSecretString !is SecretString.Unencrypted)
                                encryptionKey(realmEncryptionKey)
                        },
                        databasePassword =
                        if (realmEncryptionKeyAsSecretString !is SecretString.Unencrypted) realmEncryptionKeyAsSecretString
                        else null,
                    )
                }

            override suspend fun load(
                userId: UserId,
                databasePassword: SecretString?,
            ): Module =
                withContext(Dispatchers.IO) {
                    val rawDatabasePassword = databasePassword?.let { convertSecretString(it) }
                    createRealmRepositoriesModule {
                        directory(dbFolder(userId))
                        if (rawDatabasePassword != null)
                            encryptionKey(rawDatabasePassword.decodeHex().toByteArray())
                    }
                }

            private fun dbFolder(userId: UserId) =
                paths.rootPath.resolve(userId.asFilesystemSafeString()).resolve("database").toString()

        }
    }
}