package de.connect2x.trixnity.messenger

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.MatrixClientConfiguration
import de.connect2x.trixnity.client.RepositoriesModule
import de.connect2x.trixnity.client.create
import de.connect2x.trixnity.clientserverapi.client.MatrixClientAuthProviderData
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.secrets.SecretByteArrays
import de.connect2x.trixnity.messenger.secrets.getDatabaseKey
import de.connect2x.trixnity.messenger.secrets.setDatabaseKey
import kotlin.coroutines.CoroutineContext

private val log = Logger("de.connect2x.trixnity.messenger.MatrixClientFactory")

interface MatrixClientFactory {
    suspend fun create(
        userId: UserId,
        authProviderData: MatrixClientAuthProviderData,
        configuration: MatrixClientConfiguration.() -> Unit,
    ): Result<MatrixClient>

    suspend fun load(
        userId: UserId,
        authProviderData: MatrixClientAuthProviderData? = null,
        configuration: MatrixClientConfiguration.() -> Unit,
    ): Result<MatrixClient>
}

class MatrixClientFactoryImpl(
    private val secretByteArrays: SecretByteArrays,
    private val createRepositoriesModule: CreateRepositoriesModule,
    private val createMediaStoreModule: CreateMediaStoreModule,
    private val createCryptoDriverModule: CreateCryptoDriverModule,
    private val appCoroutineContext: CoroutineContext,
) : MatrixClientFactory {
    override suspend fun create(
        userId: UserId,
        authProviderData: MatrixClientAuthProviderData,
        configuration: MatrixClientConfiguration.() -> Unit
    ): Result<MatrixClient> =
        MatrixClient.create(
            repositoriesModule = createRepositoriesModuleOrThrow(userId),
            mediaStoreModule = createMediaStoreModule(userId),
            cryptoDriverModule = createCryptoDriverModule(),
            authProviderData = authProviderData,
            coroutineContext = appCoroutineContext,
            configuration = configuration,
        )

    override suspend fun load(
        userId: UserId,
        authProviderData: MatrixClientAuthProviderData?,
        configuration: MatrixClientConfiguration.() -> Unit
    ): Result<MatrixClient> =
        MatrixClient.create(
            repositoriesModule = loadRepositoriesModuleOrThrow(userId),
            mediaStoreModule = createMediaStoreModule(userId),
            cryptoDriverModule = createCryptoDriverModule(),
            authProviderData = authProviderData,
            coroutineContext = appCoroutineContext,
            configuration = configuration,
        )


    private suspend fun createRepositoriesModuleOrThrow(
        userId: UserId,
    ): RepositoriesModule {
        log.debug { "create repositories module" }
        val repositoriesModule = try {
            createRepositoriesModule.create(userId, getDatabaseKey(userId, true))
        } catch (exc: Exception) {
            if (isLocked(exc)) throw MatrixClientInitializationException.DatabaseLockedException()
            else throw MatrixClientInitializationException.DatabaseAccessException(exc.message)
        }
        return repositoriesModule
    }

    private suspend fun loadRepositoriesModuleOrThrow(
        userId: UserId,
    ): RepositoriesModule {
        log.debug { "load repositories module" }
        val repositoriesModule = try {
            createRepositoriesModule.load(userId, getDatabaseKey(userId, false))
        } catch (exc: Exception) {
            if (isLocked(exc)) throw MatrixClientInitializationException.DatabaseLockedException()
            else throw MatrixClientInitializationException.DatabaseAccessException(exc.message)
        }
        return repositoriesModule
    }

    private suspend fun getDatabaseKey(userId: UserId, createNew: Boolean): ByteArray? {
        return if (createNew) {
            val newKey = createRepositoriesModule.generateDatabaseKey() ?: return null
            secretByteArrays.setDatabaseKey(userId, newKey)
            newKey
        } else {
            secretByteArrays.getDatabaseKey(userId)
        }
    }

    // we cannot check for SQLNonTransientConnectionException since this is common code
    private fun isLocked(exc: Throwable): Boolean =
        exc.cause?.message?.contains("locked") == true || exc.cause?.let { isLocked(it) } ?: false
}
