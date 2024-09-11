package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.MatrixClientFactory.LoginResult
import de.connect2x.trixnity.messenger.util.SecretByteArray
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.MatrixClient.LoginInfo
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.loginWith
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module


private val log = KotlinLogging.logger { }

interface MatrixClientFactory {
    data class LoginResult(
        val matrixClient: MatrixClient,
        val databasePassword: SecretByteArray?,
    )

    suspend fun loginWith(
        baseUrl: Url,
        getLoginInfo: suspend (MatrixClientServerApiClient) -> LoginInfo,
        checkExisting: suspend (LoginInfo) -> Unit,
    ): Result<LoginResult>

    suspend fun initFromStore(
        userId: UserId,
        databasePassword: SecretByteArray?,
    ): Result<MatrixClient?>
}

class MatrixClientFactoryImpl(
    private val repositoriesModuleCreation: CreateRepositoriesModule,
    private val createMediaStore: CreateMediaStore,
    private val configuration: CreateMatrixClientConfiguration,
    private val onLogin: suspend (loginInfo: LoginInfo, baseUrl: Url) -> Unit = { _, _ -> },
) : MatrixClientFactory {

    override suspend fun loginWith(
        baseUrl: Url,
        getLoginInfo: suspend (MatrixClientServerApiClient) -> LoginInfo,
        checkExisting: suspend (LoginInfo) -> Unit,
    ): Result<LoginResult> = kotlin.runCatching {
        log.debug { "loginWith to account" }
        var databasePassword: SecretByteArray? = null
        LoginResult(
            matrixClient = MatrixClient.loginWith(
                baseUrl = baseUrl,
                repositoriesModuleFactory = { loginInfo ->
                    checkExisting(loginInfo)
                    createRepositoriesModuleOrThrow(loginInfo.userId).also {
                        databasePassword = it.databasePassword
                    }.module
                },
                mediaStoreFactory = { loginInfo ->
                    checkExisting(loginInfo)
                    createMediaStore(loginInfo.userId)
                },
                getLoginInfo = {
                    kotlin.runCatching {
                        val loginInfo = getLoginInfo(it)
                        checkExisting(loginInfo)
                        onLogin(loginInfo, baseUrl)
                        loginInfo
                    }
                }
            ).getOrThrow(),
            databasePassword = databasePassword,
        )
    }

    override suspend fun initFromStore(
        userId: UserId,
        databasePassword: SecretByteArray?,
    ): Result<MatrixClient?> = kotlin.runCatching {
        log.debug { "initFromStore (userId=$userId)" }
        MatrixClient.fromStore(
            repositoriesModule = loadRepositoriesModuleOrThrow(userId, databasePassword),
            mediaStore = createMediaStore(userId),
            configuration = configuration(),
        ).getOrThrow()
    }

    private suspend fun createRepositoriesModuleOrThrow(
        userId: UserId,
    ): CreateRepositoriesModule.CreateResult {
        log.debug { "create repositories module" }
        val repositoriesModule = try {
            repositoriesModuleCreation.create(userId)
        } catch (exc: Exception) {
            if (isLocked(exc)) throw LoadStoreException.StoreLockedException()
            else throw LoadStoreException.StoreAccessException(exc.message)
        }
        return repositoriesModule
    }

    private suspend fun loadRepositoriesModuleOrThrow(
        userId: UserId,
        databasePassword: SecretByteArray?,
    ): Module {
        log.debug { "load repositories module" }
        val repositoriesModule = try {
            repositoriesModuleCreation.load(userId, databasePassword)
        } catch (exc: Exception) {
            if (isLocked(exc)) throw LoadStoreException.StoreLockedException()
            else throw LoadStoreException.StoreAccessException(exc.message)
        }
        return repositoriesModule
    }


    // we cannot check for SQLNonTransientConnectionException since this is common code
    private fun isLocked(exc: Throwable): Boolean =
        exc.cause?.message?.contains("locked") == true || exc.cause?.let { isLocked(it) } ?: false
}
