package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.secrets.SecretByteArrays
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.MatrixClient.LoginInfo
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.loginWith
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module
import kotlin.coroutines.CoroutineContext


private val log = KotlinLogging.logger { }

interface MatrixClientFactory {
    suspend fun loginWith(
        baseUrl: Url,
        getLoginInfo: suspend (MatrixClientServerApiClient) -> LoginInfo,
        checkExisting: suspend (LoginInfo) -> Unit,
    ): Result<MatrixClient>

    suspend fun initFromStore(
        userId: UserId,
    ): Result<MatrixClient?>
}

class MatrixClientFactoryImpl(
    private val repositoriesModuleCreation: CreateRepositoriesModule,
    private val createMediaStoreModule: CreateMediaStoreModule,
    private val settings: MatrixMessengerSettingsHolder,
    private val secretByteArrays: SecretByteArrays,
    private val configurer: List<ConfigureMatrixClientConfiguration>,
    private val coroutineContext: CoroutineContext,
    private val onLogin: suspend (loginInfo: LoginInfo, baseUrl: Url) -> Unit = { _, _ -> },
) : MatrixClientFactory {
    override suspend fun loginWith(
        baseUrl: Url,
        getLoginInfo: suspend (MatrixClientServerApiClient) -> LoginInfo,
        checkExisting: suspend (LoginInfo) -> Unit,
    ): Result<MatrixClient> = kotlin.runCatching {
        log.debug { "loginWith to account" }
        MatrixClient.loginWith(
            baseUrl = baseUrl,
            repositoriesModuleFactory = { loginInfo ->
                createRepositoriesModuleOrThrow(loginInfo.userId, getDatabaseKey(loginInfo.userId, true))
            },
            mediaStoreModuleFactory = { loginInfo ->
                createMediaStoreModule(loginInfo.userId)
            },
            getLoginInfo = {
                kotlin.runCatching {
                    val loginInfo = getLoginInfo(it)
                    checkExisting(loginInfo)
                    onLogin(loginInfo, baseUrl)
                    loginInfo
                }
            },
            configuration = {
                configurer.forEach { with(it) { invoke() } }
            },
            coroutineContext = coroutineContext,
        ).getOrThrow()
    }

    override suspend fun initFromStore(
        userId: UserId,
    ): Result<MatrixClient?> = kotlin.runCatching {
        log.debug { "initFromStore (userId=$userId)" }
        MatrixClient.fromStore(
            repositoriesModule = loadRepositoriesModuleOrThrow(userId, getDatabaseKey(userId, false)),
            mediaStoreModule = createMediaStoreModule(userId),
            configuration = {
                configurer.forEach { with(it) { invoke() } }
            },
            coroutineContext = coroutineContext,
        ).getOrThrow()
    }

    private suspend fun createRepositoriesModuleOrThrow(
        userId: UserId,
        databaseKey: ByteArray?,
    ): Module {
        log.debug { "create repositories module" }
        val repositoriesModule = try {
            repositoriesModuleCreation.create(userId, databaseKey)
        } catch (exc: Exception) {
            if (isLocked(exc)) throw MatrixClientInitializationException.DatabaseLockedException()
            else throw MatrixClientInitializationException.DatabaseAccessException(exc.message)
        }
        return repositoriesModule
    }

    private suspend fun loadRepositoriesModuleOrThrow(
        userId: UserId,
        databaseKey: ByteArray?,
    ): Module {
        log.debug { "load repositories module" }
        val repositoriesModule = try {
            repositoriesModuleCreation.load(userId, databaseKey)
        } catch (exc: Exception) {
            if (isLocked(exc)) throw MatrixClientInitializationException.DatabaseLockedException()
            else throw MatrixClientInitializationException.DatabaseAccessException(exc.message)
        }
        return repositoriesModule
    }

    companion object {
        private const val ID = "de.connect2x.trixnity.messenger.secrets.database"
    }

    private suspend fun getDatabaseKey(userId: UserId, createNew: Boolean): ByteArray? {
        return if (createNew) {
            val newKey = repositoriesModuleCreation.generateDatabaseKey() ?: return null
            secretByteArrays.set("$ID-$userId", newKey)
            newKey
        } else {
            secretByteArrays.get("$ID-$userId") ?: secretByteArrays.get(ID)
        }
    }

    // we cannot check for SQLNonTransientConnectionException since this is common code
    private fun isLocked(exc: Throwable): Boolean =
        exc.cause?.message?.contains("locked") == true || exc.cause?.let { isLocked(it) } ?: false
}
