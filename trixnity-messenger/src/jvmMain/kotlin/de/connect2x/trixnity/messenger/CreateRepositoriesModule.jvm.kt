package de.connect2x.trixnity.messenger

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.use
import de.connect2x.trixnity.messenger.MatrixClientInitializationException.DatabaseAccessException
import de.connect2x.trixnity.messenger.util.ConvertSecretByteArray
import de.connect2x.trixnity.messenger.util.RootPath
import de.connect2x.trixnity.messenger.util.SecretByteArray
import net.folivo.trixnity.client.store.repository.room.TrixnityRoomDatabase
import net.folivo.trixnity.client.store.repository.room.createRoomRepositoriesModule
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.crypto.core.SecureRandom
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> {
        val rootPath = get<RootPath>()
        val convertSecretByteArray = get<ConvertSecretByteArray>()

        object : CreateRepositoriesModule {
            override suspend fun create(userId: UserId): CreateRepositoriesModule.CreateResult {
                // TODO: figure out key size: sqlite3mc encrypts via passphrase and internally uses a kdf depending on cipher
                val databaseKey = SecureRandom.nextBytes(64)
                return CreateRepositoriesModule.CreateResult(
                    module = createRoomRepositoriesModule(db(userId, databaseKey)),
                    databaseKey = convertSecretByteArray(databaseKey)
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
                    setDriver(databaseKey?.let(::EncryptedSQLiteDriver) ?: throw DatabaseAccessException("No Encryption Key given"))
                }
        }
    }
}

private class EncryptedSQLiteDriver(key: ByteArray) : SQLiteDriver {

    @ExperimentalStdlibApi
    private val hexKey = key.toHexString()

    private val driver = BundledSQLiteDriver()

    @ExperimentalStdlibApi
    override fun open(fileName: String): SQLiteConnection =
        driver.open(fileName).apply {
            prepare("PRAGMA hexkey = '$hexKey'").use {
                if (!it.step() || it.getColumnNames().getOrNull(0) != "ok")
                    throw DatabaseAccessException("Database does not support Encryption")
            }
        }
}
