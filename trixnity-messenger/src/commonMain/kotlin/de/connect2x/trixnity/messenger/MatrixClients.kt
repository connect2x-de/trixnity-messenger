package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.MatrixClients.InitFromStoreResult
import de.connect2x.trixnity.messenger.util.DeleteAccountData
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.MatrixClient.LoginInfo
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.UserId

private val log = KotlinLogging.logger { }

interface MatrixClients : StateFlow<Map<UserId, MatrixClient>> {
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
        val failures: Map<UserId, Throwable?>,
    )

    suspend fun initFromStore(): InitFromStoreResult

    suspend fun logout(userId: UserId): Result<Unit>

    suspend fun remove(userId: UserId): Result<Unit>
}

data class AccountAlreadyExistsException(val userId: UserId) :
    IllegalStateException("account $userId already exists locally")

@OptIn(ExperimentalCoroutinesApi::class)
class MatrixClientsImpl(
    private val factory: MatrixClientFactory,
    private val deleteAccountData: DeleteAccountData,
    private val settings: MatrixMessengerSettingsHolder,
    private val config: MatrixMessengerConfiguration,
    coroutineScope: CoroutineScope,
    private val matrixClients: MutableStateFlow<Map<UserId, MatrixClient>> = MutableStateFlow(mapOf()),
    private val matrixClientServerApiClientFactory: (baseUrl: Url, accessToken: String) -> MatrixClientServerApiClient =
        { baseUrl, accessToken ->
            MatrixClientServerApiClientImpl(baseUrl = baseUrl)
                .apply { this.accessToken.value = accessToken }
        }
) : MatrixClients, StateFlow<Map<UserId, MatrixClient>> by matrixClients {
    init {
        coroutineScope.launch {
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
                            MatrixClient.LoginState.LOGGED_OUT -> {
                                remove(userId)
                            }

                            MatrixClient.LoginState.LOGGED_IN, null -> {}
                        }
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
        factory.login(
            baseUrl = baseUrl,
            identifier = identifier,
            password = password,
            initialDeviceDisplayName = initialDeviceDisplayName,
            checkExisting = { checkExisting(it, baseUrl) },
        ).map {
            applyLogin(it)
            it.matrixClient
        }

    override suspend fun login(
        baseUrl: Url,
        token: String,
        initialDeviceDisplayName: String?,
    ): Result<MatrixClient> =
        factory.login(
            baseUrl = baseUrl,
            token = token,
            initialDeviceDisplayName = initialDeviceDisplayName,
            checkExisting = { checkExisting(it, baseUrl) },
        ).map {
            applyLogin(it)
            it.matrixClient
        }

    override suspend fun loginWith(
        baseUrl: Url,
        loginInfo: LoginInfo,
    ): Result<MatrixClient> =
        factory.loginWith(
            baseUrl = baseUrl,
            loginInfo = loginInfo,
            checkExisting = { checkExisting(it, baseUrl) },
        ).map {
            applyLogin(it)
            it.matrixClient
        }

    private suspend fun applyLogin(loginResult: MatrixClientFactory.LoginResult) {
        val (matrixClient, databasePassword) = loginResult
        val displayColor =
            config.generateInitialAccountColor?.let { generateInitialAccountColor ->
                generateInitialAccountColor(
                    settings.value.base.accounts.map { it.value.base.displayColor }.filterNotNull().toSet()
                )
            }
        settings.update<MatrixMessengerAccountSettingsBase>(matrixClient.userId) {
            MatrixMessengerAccountSettingsBase.withConfigDefaults(
                databasePassword = databasePassword,
                displayColor = displayColor,
                config = config
            )
        }
        if (settings.value.base.accounts.size == 1) { // if first account, set as the active account
            settings.update<MatrixMessengerSettingsBase> { it.copy(selectedAccount = matrixClient.userId) }
        }
        matrixClients.update { it + (matrixClient.userId to matrixClient) }
    }

    private suspend fun checkExisting(loginInfo: LoginInfo, baseUrl: Url) {
        if (value.containsKey(loginInfo.userId)) {
            matrixClientServerApiClientFactory(baseUrl, loginInfo.accessToken)
                .authentication.logout()
                .onFailure { log.error(it) { "could not logout of duplicate account" } }
                .getOrNull()
            throw AccountAlreadyExistsException(loginInfo.userId)
        }
    }

    override suspend fun initFromStore(): InitFromStoreResult = coroutineScope {
        val success = MutableStateFlow(setOf<UserId>())
        val failures = MutableStateFlow(mapOf<UserId, Throwable?>())
        val newMatrixClients = settings.value.base.accounts.map { (userId, accountSettings) ->
            async {
                if (matrixClients.value[userId] == null) {
                    val newMatrixClient = factory.initFromStore(userId, accountSettings.base.databasePassword)
                        .fold(
                            onSuccess = { newMatrixClient ->
                                if (newMatrixClient != null) success.update { it + userId }
                                else failures.update { it + (userId to null) }
                                newMatrixClient
                            },
                            onFailure = { e ->
                                log.error(e) { "could not load $userId from store" }
                                failures.update { it + (userId to e) }
                                null
                            }
                        )
                    if (newMatrixClient != null) userId to newMatrixClient
                    else null
                } else null
            }
        }.awaitAll().filterNotNull()
        matrixClients.update { it + newMatrixClients }
        InitFromStoreResult(success = success.value, failures = failures.value)
    }

    override suspend fun logout(userId: UserId): Result<Unit> {
        log.info { "logout (userId=$userId)" }
        return matrixClients.value[userId]?.let { matrixClient ->
            withContext(NonCancellable) {
                matrixClient.logout()
                    .onFailure {
                        log.error(it) { "could not logout $userId" }
                    }
                remove(userId)
            }
        } ?: Result.success(Unit)
    }

    override suspend fun remove(userId: UserId): Result<Unit> = kotlin.runCatching {
        matrixClients.value[userId]?.let { matrixClient ->
            log.info { "delete account data on this machine" }
            withContext(NonCancellable) {
                matrixClient.stop(wait = true)
                settings.delete(matrixClient.userId)
                matrixClients.update { it - userId }
                deleteAccountData(userId)
            }
        }
    }
}
