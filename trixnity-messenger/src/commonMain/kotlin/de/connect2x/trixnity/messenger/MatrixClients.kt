package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.MatrixClients.InitFromStoreResult
import de.connect2x.trixnity.messenger.secrets.SecretByteArrays
import de.connect2x.trixnity.messenger.secrets.deleteDatabaseKey
import de.connect2x.trixnity.messenger.util.DeleteAccountData
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.MatrixClient.LoginInfo
import net.folivo.trixnity.clientserverapi.client.MatrixAuthProvider
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.clientserverapi.client.classicInMemory
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.authentication.LoginType
import net.folivo.trixnity.core.model.UserId

private val log = KotlinLogging.logger { }

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
interface MatrixClients : StateFlow<Map<UserId, MatrixClient>>, AutoCloseable {
    suspend fun login(
        baseUrl: Url,
        identifier: IdentifierType,
        password: String,
        initialDeviceDisplayName: String?,
    ): Result<MatrixClient>

    suspend fun login(
        baseUrl: Url,
        token: String,
        initialDeviceDisplayName: String?,
    ): Result<MatrixClient>

    suspend fun loginWith(
        baseUrl: Url,
        loginInfo: LoginInfo,
    ): Result<MatrixClient>

    data class InitFromStoreResult(
        val success: Set<UserId>,
        val failures: Map<UserId, MatrixClientInitializationException>,
    )

    val initFromStoreResult: StateFlow<InitFromStoreResult?>
    val isInitialized: StateFlow<Boolean>

    @Deprecated("This should not be used anymore. It is called automatically on startup. Instead listen to initFromStoreResult or isInitialized when you need it.")
    suspend fun initFromStore(): InitFromStoreResult

    suspend fun logout(userId: UserId): Result<Unit>

    suspend fun remove(userId: UserId): Result<Unit>
}

data class AccountAlreadyExistsException(val userId: UserId) :
    IllegalStateException("account $userId already exists locally")

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalForInheritanceCoroutinesApi::class)
class MatrixClientsImpl(
    private val factory: MatrixClientFactory,
    private val deleteAccountData: DeleteAccountData,
    private val settings: MatrixMessengerSettingsHolder,
    private val config: MatrixMessengerConfiguration,
    private val secretByteArrays: SecretByteArrays,
    private val matrixClients: MutableStateFlow<Map<UserId, MatrixClient>> = MutableStateFlow(mapOf()),
) : Worker, MatrixClients, StateFlow<Map<UserId, MatrixClient>> by matrixClients {
    override suspend fun doWork() {
        @Suppress("DEPRECATION")
        initFromStore()
        flatMapLatest { matrixClients ->
            combine(
                matrixClients.map { matrixClient -> matrixClient.value.loginState.map { matrixClient.key to it } }
            ) { it.toMap() }
        }.distinctUntilChanged()
            .collect { matrixClientLoginStates ->
                log.debug { "check login states $matrixClientLoginStates" }
                matrixClientLoginStates.forEach { (userId, loginState) ->
                    when (loginState) {
                        MatrixClient.LoginState.LOGGED_OUT_SOFT, // TODO soft logout
                        MatrixClient.LoginState.LOCKED, // TODO locked
                        MatrixClient.LoginState.LOGGED_OUT -> {
                            log.info { "remote logout triggered" }
                            remove(userId)
                        }

                        MatrixClient.LoginState.LOGGED_IN, null -> {}
                    }
                }
            }
    }

    override suspend fun login(
        baseUrl: Url,
        identifier: IdentifierType,
        password: String,
        initialDeviceDisplayName: String?,
    ): Result<MatrixClient> =
        factory.loginWith(
            baseUrl = baseUrl,
            getLoginInfo = { api ->
                api.authentication.login(
                    identifier = identifier,
                    password = password,
                    type = LoginType.Password,
                    initialDeviceDisplayName = initialDeviceDisplayName,
                    refreshToken = config.useRefreshTokens,
                ).getOrThrow().let { login ->
                    LoginInfo(
                        userId = login.userId,
                        accessToken = login.accessToken,
                        refreshToken = login.refreshToken,
                        deviceId = login.deviceId,
                    )
                }
            },
            checkExisting = { checkExisting(it, baseUrl) },
        ).map {
            applyLogin(it)
            it
        }

    override suspend fun login(
        baseUrl: Url,
        token: String,
        initialDeviceDisplayName: String?,
    ): Result<MatrixClient> =
        factory.loginWith(
            baseUrl = baseUrl,
            getLoginInfo = { api ->
                api.authentication.login(
                    token = token,
                    type = LoginType.Token(),
                    initialDeviceDisplayName = initialDeviceDisplayName,
                    refreshToken = config.useRefreshTokens,
                ).getOrThrow().let { login ->
                    LoginInfo(
                        userId = login.userId,
                        accessToken = login.accessToken,
                        refreshToken = login.refreshToken,
                        deviceId = login.deviceId,
                    )
                }
            },
            checkExisting = { checkExisting(it, baseUrl) },
        ).map {
            applyLogin(it)
            it
        }

    override suspend fun loginWith(
        baseUrl: Url,
        loginInfo: LoginInfo,
    ): Result<MatrixClient> =
        factory.loginWith(
            baseUrl = baseUrl,
            getLoginInfo = { loginInfo },
            checkExisting = { checkExisting(it, baseUrl) },
        ).map {
            applyLogin(it)
            it
        }

    private suspend fun applyLogin(matrixClient: MatrixClient) {
        val displayColor =
            config.generateInitialAccountColor?.let { generateInitialAccountColor ->
                generateInitialAccountColor(
                    settings.value.base.accounts.map { it.value.base.displayColor }.filterNotNull().toSet()
                )
            }
        settings.create(
            userId = matrixClient.userId,
            settings = MatrixMessengerAccountSettingsBase.withConfigDefaults(
                displayColor = displayColor,
                config = config
            )
        )
        if (settings.value.base.accounts.size == 1) { // if first account, set as the active account
            settings.update<MatrixMessengerSettingsBase> { it.copy(selectedAccount = matrixClient.userId) }
        }
        matrixClients.update { it + (matrixClient.userId to matrixClient) }
    }

    private suspend fun checkExisting(loginInfo: LoginInfo, baseUrl: Url) {
        if (value.containsKey(loginInfo.userId)) {
            log.debug { "account ${loginInfo.userId} already exist -> logout" }
            MatrixClientServerApiClientImpl(
                baseUrl,
                authProvider = MatrixAuthProvider.classicInMemory(
                    accessToken = loginInfo.accessToken,
                    refreshToken = loginInfo.refreshToken
                ),
                httpClientEngine = config.httpClientEngine,
                httpClientConfig = config.httpClientConfig
            ).use {
                it.authentication.logout()
            }
                .onFailure { log.error(it) { "could not logout of duplicate account" } }
                .getOrNull()
            throw AccountAlreadyExistsException(loginInfo.userId)
        } else {
            log.debug { "account ${loginInfo.userId} does not exist -> delete possible stale data" }
            remove(loginInfo.userId)
        }
    }

    private val _initFromStoreResult: MutableStateFlow<InitFromStoreResult?> = MutableStateFlow(null)
    override val initFromStoreResult: StateFlow<InitFromStoreResult?> = _initFromStoreResult.asStateFlow()
    private val _isInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    @Deprecated("This should not be used anymore. It is called automatically on startup. Instead listen to initFromStoreResult or isInitialized when you need it.")
    override suspend fun initFromStore(): InitFromStoreResult = // TODO make internal and undeprecate
        coroutineScope {
            val success = MutableStateFlow(setOf<UserId>())
            val failures = MutableStateFlow(mapOf<UserId, MatrixClientInitializationException>())
            val newMatrixClients = settings.value.base.accounts.keys.map { account ->
                async {
                    if (matrixClients.value[account] == null) {
                        val newMatrixClient = factory.initFromStore(account)
                            .fold(
                                onSuccess = { newMatrixClient ->
                                    if (newMatrixClient != null) success.update { it + account }
                                    else failures.update { it + (account to MatrixClientInitializationException.NoDatabaseException) }
                                    newMatrixClient
                                },
                                onFailure = { e ->
                                    log.error(e) { "could not load $account from store" }
                                    val failure = when (e) {
                                        is CancellationException -> throw e
                                        is MatrixClientInitializationException -> e
                                        else -> MatrixClientInitializationException.Unknown(e.message)
                                    }
                                    failures.update { it + (account to failure) }
                                    null
                                }
                            )
                        if (newMatrixClient != null) account to newMatrixClient
                        else null
                    } else null
                }
            }.awaitAll().filterNotNull()
            matrixClients.update { it + newMatrixClients }
            val result = InitFromStoreResult(success = success.value, failures = failures.value)
            _initFromStoreResult.value = result
            _isInitialized.value = true
            result
        }

    override suspend fun logout(userId: UserId): Result<Unit> {
        log.info { "logout (userId=$userId)" }
        return matrixClients.value[userId]?.let { matrixClient ->
            withContext(NonCancellable) {
                // If we fail to log out, we do not want to just blindly remove the userID,
                // because that likely fails as well. Callers know better how to handle the situation.
                // (see trixnity-messenger/src/commonMain/kotlin/de/connect2x/trixnity/messenger/viewmodel/connecting/RemoveMatrixAccountViewModel.kt)
                // If we succeed to log out, we try to remove the userid and transparently bubble the
                // result.
                matrixClient.logout().fold(
                    onSuccess = { remove(userId) },
                    onFailure = { remove(userId) }
                )
            }
        } ?: Result.success(Unit)
    }

    override suspend fun remove(userId: UserId): Result<Unit> = kotlin.runCatching {
        withContext(NonCancellable) {
            log.info { "delete account data on this machine" }
            val matrixClient = matrixClients.value[userId]
            matrixClient?.closeSuspending()

            settings.delete(userId)
            matrixClients.update { it - userId }
            secretByteArrays.deleteDatabaseKey(userId)
            deleteAccountData(userId)
        }
    }.onFailure {
        log.warn(it) { "failed to remove user data fro $userId" }
    }

    override fun close() {
        value.values.forEach { it.close() }
    }
}
