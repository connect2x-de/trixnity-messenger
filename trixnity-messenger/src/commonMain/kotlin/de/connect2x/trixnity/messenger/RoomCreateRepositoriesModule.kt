package de.connect2x.trixnity.messenger

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteException
import de.connect2x.sqlitenity.encrypted.driver.EncryptedSQLiteDriver
import de.connect2x.sqlitenity.encrypted.driver.EncryptionKey
import de.connect2x.sqlitenity.encrypted.driver.SQLITE_BUSY
import de.connect2x.sqlitenity.encrypted.driver.SQLITE_LOCKED
import de.connect2x.sqlitenity.encrypted.driver.SQLITE_NOTADB_AUTH_FAILED
import de.connect2x.sqlitenity.encrypted.driver.errorCode
import de.connect2x.sqlitenity.encrypted.driver.extendedErrorCode
import de.connect2x.trixnity.client.RepositoriesModule
import de.connect2x.trixnity.client.store.repository.room.TrixnityRoomDatabase
import de.connect2x.trixnity.client.store.repository.room.room
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.crypto.core.SecureRandom
import de.connect2x.trixnity.messenger.util.RootPath
import okio.FileSystem
import org.koin.core.scope.Scope

internal class RoomCreateRepositoriesModule(
    private val rootPath: RootPath,
    private val fileSystem: FileSystem?,
    private val matrixMessengerConfiguration: MatrixMessengerConfiguration,
    private val scope: Scope,
) : CreateRepositoriesModule {
    override suspend fun generateDatabaseKey(): ByteArray? =
        if (matrixMessengerConfiguration.databaseEncryptionEnabled)
            SecureRandom.nextBytes(EncryptionKey.PlaintextHeader.SIZE)
        else null

    override suspend fun create(userId: UserId, databaseKey: ByteArray?): RepositoriesModule {
        fileSystem?.createDirectories(rootPath.forAccountDatabase(userId), mustCreate = false)
        return RepositoriesModule.room(db(userId, databaseKey))
    }

    override suspend fun load(userId: UserId, databaseKey: ByteArray?): RepositoriesModule {
        return RepositoriesModule.room(db(userId, databaseKey))
    }

    override fun handleExceptions(exc: Exception) {
        if (exc !is SQLiteException) return

        if (exc.errorCode == SQLITE_BUSY || exc.errorCode == SQLITE_LOCKED)
            throw MatrixClientInitializationException.DatabaseLockedException(exc.message)

        if (exc.extendedErrorCode == SQLITE_NOTADB_AUTH_FAILED)
            throw MatrixClientInitializationException.DatabaseLockedException(exc.message)
    }

    private fun db(userId: UserId, databaseKey: ByteArray?): RoomDatabase.Builder<TrixnityRoomDatabase> =
        scope
            .roomDatabaseBuilder<TrixnityRoomDatabase>(
                rootPath.forAccountDatabase(userId).resolve("database").toString()
            )
            .setDriver(EncryptedSQLiteDriver(EncryptionKey(databaseKey)))
}

internal expect inline fun <reified T : RoomDatabase> Scope.roomDatabaseBuilder(name: String): RoomDatabase.Builder<T>
