package de.connect2x.trixnity.messenger

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlitemc.driver.bundled.BundledSQLiteDriver
import de.connect2x.trixnity.messenger.MatrixClientInitializationException.DatabaseAccessException
import de.connect2x.trixnity.messenger.util.RootPath
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.store.repository.room.TrixnityRoomDatabase
import net.folivo.trixnity.client.store.repository.room.createRoomRepositoriesModule
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.crypto.core.SecureRandom
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> {
        val rootPath = get<RootPath>()
        val fileSystem = get<FileSystem>()

        object : CreateRepositoriesModule {
            override suspend fun generateDatabaseKey(): ByteArray =
                SecureRandom.nextBytes(EncryptedSQLiteDriver.KEY_SIZE)

            override suspend fun create(userId: UserId, databaseKey: ByteArray?): Module {
                fileSystem.createDirectories(rootPath.forAccountDatabase(userId), mustCreate = false)
                return createRoomRepositoriesModule(db(userId, databaseKey))
            }

            override suspend fun load(userId: UserId, databaseKey: ByteArray?): Module {
                return createRoomRepositoriesModule(db(userId, databaseKey))
            }

            private fun db(userId: UserId, databaseKey: ByteArray?): RoomDatabase.Builder<TrixnityRoomDatabase> =
                Room.databaseBuilder<TrixnityRoomDatabase>(
                    rootPath.forAccountDatabase(userId).resolve("database").toString()
                ).apply {
                    setDriver(
                        databaseKey?.let(::EncryptedSQLiteDriver)
                            ?: throw DatabaseAccessException("No Encryption Key given")
                    )
                }
        }
    }
}

private class EncryptedSQLiteDriver(key: ByteArray) : SQLiteDriver {


    companion object {
        const val KEY_SIZE = 32
        val mutex = Mutex()
    }

    init {
        if (key.size != KEY_SIZE) {
            throw DatabaseAccessException("Invalid key size: want ${KEY_SIZE}, got ${key.size}")
        }
    }

    @ExperimentalStdlibApi
    private val rawKey = key.toHexString()

    private val driver = BundledSQLiteDriver()

    @ExperimentalStdlibApi
    override fun open(fileName: String): SQLiteConnection = runBlocking {
        mutex.withLock {
            driver.open(fileName).apply {
                prepare("PRAGMA key = 'raw:$rawKey'").use {
                    if (!it.step() || it.getColumnNames().getOrNull(0) != "ok")
                        throw DatabaseAccessException("Database does not support Encryption")
                }
            }
        }
    }
}
