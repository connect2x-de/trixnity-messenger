package de.connect2x.trixnity.messenger

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.*
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.authentication.LoginType
import net.folivo.trixnity.core.model.UserId
import org.koin.core.Koin
import org.koin.core.module.Module

private val log = KotlinLogging.logger { }


interface MatrixClientFactory {
    suspend fun login(
        baseUrl: Url,
        identifier: IdentifierType,
        password: String,
        initialDeviceDisplayName: String?,
        accountName: String,
    ): Result<MatrixClient>

    suspend fun login(
        baseUrl: Url,
        token: String,
        initialDeviceDisplayName: String?,
        accountName: String,
    ): Result<MatrixClient>

    suspend fun loginWith(
        baseUrl: Url,
        userId: UserId,
        deviceId: String,
        accessToken: String,
        displayName: String?,
        avatarUrl: String? = null,
        accountName: String,
    ): Result<MatrixClient>

    suspend fun initFromStore(
        accountName: String,
    ): Result<MatrixClient?>

}

/**
 * This does not manage the CoroutineScopes. If you have a lifecycle for this service, you need to override the scope
 * and manage its lifecycle by yourself.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMatrixClientService(
    di: Koin,
    private val matrixClientFactory: () -> MatrixClientFactory = {
        log.trace { "create MatrixClientFactory" }
        val repositoriesModuleCreation = di.get<CreateRepositoriesModule>()
        val mediaStoreCreation = di.get<CreateMediaStore>()
        val configuration = di.get<CreateMatrixClientConfiguration>()()
        object : MatrixClientFactory {
            override suspend fun login(
                baseUrl: Url,
                identifier: IdentifierType,
                password: String,
                initialDeviceDisplayName: String?,
                accountName: String,
            ): Result<MatrixClient> {
                val repositoriesModule = createRepositoriesModule(repositoriesModuleCreation, accountName)
                log.debug { "MatrixClient.login" }
                return MatrixClient.login(
                    baseUrl = baseUrl,
                    identifier = identifier,
                    password = password,
                    initialDeviceDisplayName = initialDeviceDisplayName ?: deviceDisplayName(),
                    repositoriesModule = repositoriesModule,
                    mediaStore = mediaStoreCreation(accountName),
                    configuration = configuration,
                )
            }

            override suspend fun login(
                baseUrl: Url,
                token: String,
                initialDeviceDisplayName: String?,
                accountName: String,
            ): Result<MatrixClient> {
                val repositoriesModule = createRepositoriesModule(repositoriesModuleCreation, accountName)
                log.debug { "MatrixClient.login" }
                return MatrixClient.login(
                    loginType = LoginType.Token(),
                    token = token,
                    baseUrl = baseUrl,
                    initialDeviceDisplayName = initialDeviceDisplayName ?: deviceDisplayName(),
                    repositoriesModule = repositoriesModule,
                    mediaStore = mediaStoreCreation(accountName),
                    configuration = configuration,
                )
            }

            override suspend fun loginWith(
                baseUrl: Url,
                userId: UserId,
                deviceId: String,
                accessToken: String,
                displayName: String?,
                avatarUrl: String?,
                accountName: String,
            ): Result<MatrixClient> {
                val repositoriesModule = createRepositoriesModule(repositoriesModuleCreation, accountName)
                log.debug { "MatrixClient.loginWith" }
                return MatrixClient.loginWith(
                    baseUrl,
                    repositoriesModule,
                    mediaStoreCreation(accountName),
                    getLoginInfo = {
                        Result.success(
                            MatrixClient.LoginInfo(
                                userId = userId,
                                deviceId = deviceId,
                                accessToken = accessToken,
                                displayName = displayName,
                                avatarUrl = avatarUrl,
                            )
                        )
                    })
            }

            override suspend fun initFromStore(
                accountName: String,
            ): Result<MatrixClient?> {
                val repositoriesModule = try {
                    repositoriesModuleCreation(accountName)
                } catch (exc: Exception) {
                    throw LoadStoreException.StoreAccessException(exc)
                }
                return MatrixClient.fromStore(
                    repositoriesModule = repositoriesModule,
                    mediaStore = mediaStoreCreation(accountName),
                    configuration = configuration,
                )
            }

            private suspend fun createRepositoriesModule(
                repositoriesModuleCreation: CreateRepositoriesModule,
                accountName: String
            ): Module {
                log.debug { "create repositories module" }
                val repositoriesModule = try {
                    repositoriesModuleCreation(accountName)
                } catch (exc: Exception) {
                    throw LoadStoreException.StoreAccessException(exc)
                }
                return repositoriesModule
            }
        }
    },
    override val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : MatrixClientService {

    override val matrixClients = MutableStateFlow<List<NamedMatrixClient>>(listOf())
    private val mutex = Mutex()

    override val notificationCount = matrixClients.flatMapLatest { namedMatrixClients ->
        combine(namedMatrixClients.map { namedMatrixClient ->
            namedMatrixClient.matrixClient.value?.room?.getAll()?.flattenValues()?.map { rooms ->
                rooms.sumOf { it.unreadMessageCount }
            } ?: flowOf(0L)
        }) {
            it.sum()
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(), 0L)

    // for iOS, since default parameters are not supported in MPP for Swift (https://youtrack.jetbrains.com/issue/KT-41908/cant-instantiate-data-classes-on-iOS-init-is-unavailable)
    companion object {
        fun create(di: Koin): MatrixClientService = DefaultMatrixClientService(di)
    }

    override suspend fun login(
        baseUrl: Url,
        identifier: IdentifierType,
        password: String,
        initialDeviceDisplayName: String?,
        accountName: String,
    ): Result<Unit> {
        log.debug { "log in to account: $accountName" }
        log.debug { "existing MatrixClients: ${matrixClients.value}" }
        return if (matrixClients.value.none { it.accountName == accountName }) {
            log.info { "try to login" }
            matrixClientFactory().login(
                baseUrl,
                identifier,
                password,
                initialDeviceDisplayName,
                accountName,
            ).map {
                // if we log in, we need to register the new account name locally
                matrixClients.value += NamedMatrixClient(accountName, MutableStateFlow(it))
                log.debug { "logged in successfully with account $accountName" }
            }.recoverCatching { exc ->
                mapExceptions(exc)
            }
        } else Result.success(Unit)
    }

    override suspend fun login(
        baseUrl: Url,
        token: String,
        initialDeviceDisplayName: String?,
        accountName: String,
    ): Result<Unit> {
        log.debug { "log in to account: $accountName" }
        log.debug { "existing MatrixClients: ${matrixClients.value}" }
        return if (matrixClients.value.none { it.accountName == accountName }) {
            log.info { "try to login with token" }
            matrixClientFactory().login(
                baseUrl,
                token,
                initialDeviceDisplayName,
                accountName,
            ).map {
                // if we log in, we need to register the new account name locally
                matrixClients.value += NamedMatrixClient(accountName, MutableStateFlow(it))
                log.debug { "logged in successfully with account $accountName" }
            }.recoverCatching { exc ->
                mapExceptions(exc)
            }
        } else Result.success(Unit)
    }

    override suspend fun loginWith(
        baseUrl: Url,
        userId: UserId,
        deviceId: String,
        accessToken: String,
        displayName: String?,
        avatarUrl: String?,
        accountName: String
    ): Result<Unit> {
        log.debug { "log in to account '$accountName' with existing access token" }
        log.debug { "existing MatrixClients: ${matrixClients.value}" }
        return if (matrixClients.value.none { it.accountName == accountName }) {
            log.info { "try to loginWith" }
            matrixClientFactory().loginWith(
                baseUrl, userId, deviceId, accessToken, displayName, avatarUrl, accountName
            ).map {
                // if we log in, we need to register the new account name locally
                matrixClients.value += NamedMatrixClient(accountName, MutableStateFlow(it))
                log.debug { "logged in successfully with account $accountName" }
            }.recoverCatching { exc ->
                mapExceptions(exc)
            }
        } else Result.success(Unit)
    }


    override suspend fun logout(accountName: String): Result<Unit> {
        log.info { "logging out of account '$accountName'" }
        log.debug { "currently active MatrixClients: ${matrixClients.value.joinToString { "${it.accountName} (${it.matrixClient.value})" }}" }
        return matrixClients.value.find { it.accountName == accountName }?.let { namedMatrixClient ->
            log.info { "MatrixClient.logout() for $namedMatrixClient" }
            val result = namedMatrixClient.matrixClient.value?.logout()?.onSuccess {
                namedMatrixClient.matrixClient.value?.stop()
                namedMatrixClient.matrixClient.value = null
                log.info { "now, delete account data on this machine" }
                deleteAccountDataLocally(accountName)
                matrixClients.value -= namedMatrixClient
            }
            result
        } ?: Result.success(Unit)
    }

    override suspend fun initFromStore(
        accountName: String,
    ): Result<Boolean> {
        return mutex.withLock {
            if (matrixClients.value.none { it.accountName == accountName }) {
                log.info { "try to init from store" }
                matrixClientFactory().initFromStore(accountName).map {
                    if (it != null) {
                        matrixClients.value += NamedMatrixClient(accountName, MutableStateFlow(it))
                        true
                    } else false
                }.recoverCatching { exc ->
                    mapExceptions(exc)
                    false
                }
            } else Result.success(true)
        }
    }

    override fun destroy() {
        log.info { "destroy" }
        super.destroy()
        matrixClients.value.forEach { namedMatrixClient ->
            namedMatrixClient.matrixClient.value = null
        }
        matrixClients.value = listOf()
    }

    private fun mapExceptions(exc: Throwable) {
        when (exc) {
            is LoadStoreException.StoreAccessException ->
                if (isLocked(exc)) throw LoadStoreException.StoreLockedException() else throw exc

            else -> throw exc
        }
    }

    // we cannot check for SQLNonTransientConnectionException since this is common code
    private fun isLocked(exc: Throwable): Boolean =
        exc.cause?.message?.contains("locked") == true || exc.cause?.let { isLocked(it) } ?: false
}
