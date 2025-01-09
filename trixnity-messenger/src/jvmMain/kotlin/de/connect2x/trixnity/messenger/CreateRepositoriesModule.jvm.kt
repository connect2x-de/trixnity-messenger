package de.connect2x.trixnity.messenger

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.connect2x.trixnity.messenger.util.ConvertSecretByteArray
import de.connect2x.trixnity.messenger.util.RootPath
import de.connect2x.trixnity.messenger.util.SecretByteArray
import net.folivo.trixnity.client.store.repository.room.TrixnityRoomDatabase
import net.folivo.trixnity.client.store.repository.room.createRoomRepositoriesModule
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> {
        val rootPath = get<RootPath>()
        val convertSecretByteArray = get<ConvertSecretByteArray>()

        object : CreateRepositoriesModule {
            override suspend fun create(userId: UserId): CreateRepositoriesModule.CreateResult {
                val databaseKey: ByteArray? = null // FIXME
                return CreateRepositoriesModule.CreateResult(
                    module = createRoomRepositoriesModule(db(userId, databaseKey)),
                    databaseKey = databaseKey?.let { convertSecretByteArray(it) },
                )
            }

            override suspend fun load(
                userId: UserId,
                databaseKey: SecretByteArray?,
            ): Module {
                val existingKey = databaseKey?.let { convertSecretByteArray(it) }
                return createRoomRepositoriesModule(db(userId, existingKey))
            }

            private fun db(userId: UserId, databaseKey: ByteArray?): RoomDatabase.Builder<TrixnityRoomDatabase> =
                Room.databaseBuilder<TrixnityRoomDatabase>(
                    rootPath.forAccountDatabase(userId).resolve("database").toString()
                ).apply {
                    setDriver(BundledSQLiteDriver())
                }
        }
    }
}
