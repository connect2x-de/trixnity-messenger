package de.connect2x.trixnity.messenger

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.sqlitenity.api.SQLitenityException
import de.connect2x.sqlitenity.bundled.SQLitenityBundledDriver
import de.connect2x.sqlitenity.compat.SQLitenityCompatDriver
import de.connect2x.trixnity.client.RepositoriesModule
import de.connect2x.trixnity.client.store.repository.room.TrixnityRoomDatabase
import de.connect2x.trixnity.client.store.repository.room.room
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.crypto.core.SecureRandom
import de.connect2x.trixnity.messenger.MatrixClientInitializationException.DatabaseAccessException
import de.connect2x.trixnity.messenger.util.RootPath
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module

actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> {
        val rootPath = get<RootPath>()
        val fileSystem = get<FileSystem>()
        val databaseEncryptionEnabled = get<MatrixMessengerConfiguration>().databaseEncryptionEnabled

        object : CreateRepositoriesModule {
            override suspend fun generateDatabaseKey(): ByteArray? =
                if (databaseEncryptionEnabled)
                    SecureRandom.nextBytes(EncryptedSQLiteDriver.KEY_SIZE + EncryptedSQLiteDriver.SALT_SIZE)
                else null

            override suspend fun create(userId: UserId, databaseKey: ByteArray?): RepositoriesModule {
                fileSystem.createDirectories(rootPath.forAccountDatabase(userId), mustCreate = false)
                return RepositoriesModule.room(db(userId, databaseKey))
            }

            override suspend fun load(userId: UserId, databaseKey: ByteArray?): RepositoriesModule {
                return RepositoriesModule.room(db(userId, databaseKey))
            }

            override fun handleExceptions(exc: Exception) {
                handleSqliteExceptions(exc, databaseEncryptionEnabled)
            }

            private fun db(userId: UserId, databaseKey: ByteArray?): RoomDatabase.Builder<TrixnityRoomDatabase> =
                roomDatabaseBuilder<TrixnityRoomDatabase>(
                        rootPath.forAccountDatabase(userId).resolve("database").toString()
                    )
                    .setDriver(EncryptedSQLiteDriver(databaseKey))
        }
    }
}

internal fun handleSqliteExceptions(exc: Exception, databaseEncryptionEnabled: Boolean) {
    when (exc) {
        is SQLitenityException -> {
            // SQLite error code 26: file is encrypted or is not a database
            // 26 is the error code for SQLITE_NOTADB
            if (databaseEncryptionEnabled && exc.cause?.message?.contains("Error code: 26") == true) {
                throw MatrixClientInitializationException.DatabaseCannotBeDecryptedException(exc.cause?.message)
            }
            // otherwise: let later handlers do the work
        }
        else -> {
            // do not handle and let later handlers do the work
        }
    }
}

internal expect inline fun <reified T : RoomDatabase> Scope.roomDatabaseBuilder(name: String): RoomDatabase.Builder<T>

private class EncryptedSQLiteDriver(key: ByteArray?) : SQLiteDriver {
    private val log = Logger("de.connect2x.trixnity.messenger.EncryptedSQLiteDriver")

    companion object {
        const val KEY_SIZE = 32
        const val SALT_SIZE = 16
    }

    private val usePlaintextHeader =
        when (key?.size) {
            KEY_SIZE + SALT_SIZE -> true
            KEY_SIZE -> false
            null -> null
            else -> {
                throw DatabaseAccessException("Invalid key size: want ${KEY_SIZE}, got ${key.size}")
            }
        }

    init {
        when (usePlaintextHeader) {
            true -> log.debug { "Opening database with plaintext header" }
            false -> log.debug { "Opening database with encrypted header" }
            null -> log.debug { "Opening database without encryption" }
        }
    }

    @ExperimentalStdlibApi private val rawKey = key?.toHexString()

    private val driver = SQLitenityCompatDriver(SQLitenityBundledDriver())

    @ExperimentalStdlibApi
    override fun open(fileName: String): SQLiteConnection =
        driver.open(fileName).apply {
            if (usePlaintextHeader == true)
                prepare("PRAGMA plaintext_header_size=24").use {
                    if (!it.step() || it.getColumnNames().getOrNull(0) != "24")
                        throw DatabaseAccessException("Database does not support Plaintext Header")
                }

            if (rawKey != null)
                prepare("PRAGMA key = 'raw:$rawKey'").use {
                    if (!it.step() || it.getColumnNames().getOrNull(0) != "ok")
                        throw DatabaseAccessException("Database does not support Encryption")
                }
        }
}
