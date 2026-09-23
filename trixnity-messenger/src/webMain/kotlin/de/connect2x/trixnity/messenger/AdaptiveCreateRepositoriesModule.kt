package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.client.RepositoriesModule
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.util.RootPath
import de.connect2x.trixnity.messenger.util.isOPFSAvailable
import de.connect2x.trixnity.messenger.util.isWorkersAvailable
import okio.FileSystem
import org.koin.core.scope.Scope
import web.storage.localStorage

internal class AdaptiveCreateRepositoriesModule(
    private val rootPath: RootPath,
    private val fileSystem: FileSystem?,
    private val matrixMessengerConfiguration: MatrixMessengerConfiguration,
    private val scope: Scope,
) : CreateRepositoriesModule {

    private val roomCreateRepositoriesModule =
        RoomCreateRepositoriesModule(
            rootPath = rootPath,
            fileSystem = fileSystem,
            matrixMessengerConfiguration = matrixMessengerConfiguration,
            scope = scope,
        )

    private val indexedDBCreateRepositoriesModule = IndexedDBCreateRepositoriesModule(rootPath = rootPath)

    override suspend fun generateDatabaseKey(): ByteArray? {
        return if (canEnableRoom3()) {
            roomCreateRepositoriesModule.generateDatabaseKey()
        } else {
            indexedDBCreateRepositoriesModule.generateDatabaseKey()
        }
    }

    override suspend fun create(userId: UserId, databaseKey: ByteArray?): RepositoriesModule {
        return if (canEnableRoom3()) {
            setIsRoom3Enabled(userId)
            roomCreateRepositoriesModule.create(userId, databaseKey)
        } else {
            indexedDBCreateRepositoriesModule.create(userId, databaseKey)
        }
    }

    override suspend fun load(userId: UserId, databaseKey: ByteArray?): RepositoriesModule {
        return if (isRoom3Enabled(userId)) {
            roomCreateRepositoriesModule.load(userId, databaseKey)
        } else {
            indexedDBCreateRepositoriesModule.load(userId, databaseKey)
        }
    }

    override fun handleExceptions(exc: Exception) {
        roomCreateRepositoriesModule.handleExceptions(exc)
    }

    private fun setIsRoom3Enabled(userId: UserId) {
        localStorage.setItem(rootPath.forAccount(userId).resolve("room3Enabled").toString(), "true")
    }

    private fun isRoom3Enabled(userId: UserId): Boolean {
        return localStorage.getItem(rootPath.forAccount(userId).resolve("room3Enabled").toString()) == "true"
    }

    private suspend fun canEnableRoom3(): Boolean {
        return isWorkersAvailable() && isOPFSAvailable()
    }
}
