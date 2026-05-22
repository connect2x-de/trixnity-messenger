package de.connect2x.trixnity.messenger

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.clientserverapi.client.MatrixClientAuthProviderData
import de.connect2x.trixnity.clientserverapi.client.useApi
import de.connect2x.trixnity.core.ErrorResponse
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients.CreateResult.Failure
import de.connect2x.trixnity.messenger.MatrixClients.InitFromStoreResult
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.secrets.SecretByteArrays
import de.connect2x.trixnity.messenger.secrets.deleteDatabaseKey
import de.connect2x.trixnity.messenger.util.DeleteAccountData
import io.ktor.client.plugins.*
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
import kotlinx.serialization.Serializable

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
interface MatrixClients : StateFlow<Map<UserId, MatrixClient>>, AutoCloseable, Worker {
    data class InitFromStoreResult(
        val success: Set<UserId>,
        val failures: Map<UserId, MatrixClientInitializationException>,
    )

    val initFromStoreResult: StateFlow<InitFromStoreResult?>
    val isInitialized: StateFlow<Boolean>

    suspend fun create(authProviderData: MatrixClientAuthProviderData): CreateResult

    suspend fun logout(userId: UserId): Result<Unit>

    suspend fun remove(userId: UserId): Result<Unit>

    sealed interface CreateResult {
        data object Success : CreateResult

        sealed interface Failure : CreateResult {
            val message: String

            data class InvalidAuthentication(override val message: String) : Failure

            data class UserDeactivated(override val message: String) : Failure

            data class Connection(override val message: String) : Failure

            data class AccountAlreadyExists(override val message: String) : Failure

            data class Database(override val message: String) : Failure

            data class Unknown(override val message: String) : Failure
        }
    }
}

data class AccountAlreadyExistsException(val userId: UserId) :
    IllegalStateException("account $userId already exists locally")

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalForInheritanceCoroutinesApi::class)
class MatrixClientsImpl(
    private val matrixClientFactory: MatrixClientFactory,
    private val deleteAccountData: DeleteAccountData,
    private val settings: MatrixMessengerSettingsHolder,
    private val config: MatrixMessengerConfiguration,
    private val secretByteArrays: SecretByteArrays,
    private val i18n: I18n,
    private val matrixClients: MutableStateFlow<Map<UserId, MatrixClient>> = MutableStateFlow(mapOf()),
) : MatrixClients, StateFlow<Map<UserId, MatrixClient>> by matrixClients {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.MatrixClientsImpl")
    }

    override suspend fun doWork() {
        initFromStore()

        flatMapLatest { matrixClients ->
                combine(
                    matrixClients.map { matrixClient -> matrixClient.value.loginState.map { matrixClient.key to it } }
                ) {
                    it.toMap()
                }
            }
            .distinctUntilChanged()
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

                        MatrixClient.LoginState.LOGGED_IN,
                        null -> {}
                    }
                }
            }
    }

    override suspend fun create(authProviderData: MatrixClientAuthProviderData): MatrixClients.CreateResult =
        runCatching {
                val userId =
                    authProviderData
                        .useApi(
                            httpClientConfig = config.httpClientConfig,
                            httpClientEngine = config.httpClientEngine,
                        ) {
                            it.authentication.whoAmI()
                        }
                        .getOrThrow()
                        .userId
                checkExisting(authProviderData, userId)
                val matrixClient =
                    matrixClientFactory.create(userId = userId, authProviderData = authProviderData).getOrThrow()
                add(matrixClient)
                matrixClient
            }
            .fold(
                onSuccess = { MatrixClients.CreateResult.Success },
                onFailure = { exception ->
                    when (exception) {
                        is CancellationException -> throw exception
                        is MatrixServerException ->
                            when (exception.errorResponse) {
                                ErrorResponse.Forbidden ->
                                    Failure.InvalidAuthentication(i18n.createMatrixClientFailureInvalidAuthentication())
                                ErrorResponse.UserDeactivated ->
                                    Failure.UserDeactivated(i18n.createMatrixClientFailureUserDeactivated())
                                else -> Failure.Unknown(i18n.createMatrixClientFailureUnknown(exception.message))
                            }

                        is AccountAlreadyExistsException ->
                            Failure.AccountAlreadyExists(i18n.createMatrixClientFailureAlreadyExists(exception.userId))

                        is MatrixClientInitializationException.DatabaseLockedException ->
                            Failure.Database(i18n.createMatrixClientFailureDatabaseLocked())

                        is MatrixClientInitializationException.DatabaseAccessException ->
                            Failure.Database(i18n.createMatrixClientFailureDatabaseAccess())

                        is ResponseException ->
                            Failure.Connection(i18n.createMatrixClientFailureConnection(exception.message))

                        else -> {
                            log.warn(exception) { "unhandled exception: $exception" }
                            Failure.Unknown(i18n.createMatrixClientFailureUnknown(exception.message))
                        }
                    }
                },
            )

    private suspend fun checkExisting(authProviderData: MatrixClientAuthProviderData, userId: UserId) {
        if (value.containsKey(userId)) {
            log.debug { "account $userId already exist -> logout" }
            authProviderData
                .useApi(httpClientConfig = config.httpClientConfig, httpClientEngine = config.httpClientEngine) {
                    it.authentication.logout()
                }
                .onFailure { log.error(it) { "could not logout of duplicate account" } }
                .getOrNull()
            throw AccountAlreadyExistsException(userId)
        } else {
            log.debug { "account $userId does not exist -> delete possible stale data" }
            remove(userId)
        }
    }

    private suspend fun add(matrixClient: MatrixClient) {
        val displayColor =
            config.generateInitialAccountColor?.let { generateInitialAccountColor ->
                generateInitialAccountColor(
                    settings.value.base.accounts.map { it.value.base.displayColor }.filterNotNull().toSet()
                )
            }
        settings.create(
            userId = matrixClient.userId,
            settings =
                MatrixMessengerAccountSettingsBase.withConfigDefaults(displayColor = displayColor, config = config),
        )
        if (settings.value.base.accounts.size == 1) { // if first account, set as the active account
            settings.update<MatrixMessengerSettingsBase> { it.copy(selectedAccount = matrixClient.userId) }
        }
        matrixClients.update { it + (matrixClient.userId to matrixClient) }
    }

    private val _initFromStoreResult: MutableStateFlow<InitFromStoreResult?> = MutableStateFlow(null)
    override val initFromStoreResult: StateFlow<InitFromStoreResult?> = _initFromStoreResult.asStateFlow()
    private val _isInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    internal suspend fun initFromStore(): InitFromStoreResult = coroutineScope {
        val success = MutableStateFlow(setOf<UserId>())
        val failures = MutableStateFlow(mapOf<UserId, MatrixClientInitializationException>())
        val newMatrixClients =
            settings.value.base.accounts.keys
                .map { account ->
                    async {
                        if (matrixClients.value[account] == null) {
                            val newMatrixClient =
                                runCatching { matrixClientFactory.load(userId = account).getOrThrow() }
                                    .onSuccess { success.update { it + account } }
                                    .onFailure { e ->
                                        log.error(e) { "could not load $account from store" }
                                        val failure =
                                            when (e) {
                                                is CancellationException -> throw e
                                                is MatrixClientInitializationException -> e
                                                else -> MatrixClientInitializationException.Unknown(e.message)
                                            }
                                        failures.update { it + (account to failure) }
                                    }
                                    .getOrNull()
                            if (newMatrixClient != null) account to newMatrixClient else null
                        } else null
                    }
                }
                .awaitAll()
                .filterNotNull()
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
                // (see
                // trixnity-messenger/src/commonMain/kotlin/de/connect2x/trixnity/messenger/viewmodel/connecting/RemoveMatrixAccountViewModel.kt)
                // If we succeed to log out, we try to remove the userid and transparently bubble the
                // result.
                matrixClient.logout().fold(onSuccess = { remove(userId) }, onFailure = { remove(userId) })
            }
        } ?: Result.success(Unit)
    }

    override suspend fun remove(userId: UserId): Result<Unit> =
        kotlin
            .runCatching {
                withContext(NonCancellable) {
                    log.info { "delete account data on this machine" }
                    val matrixClient = matrixClients.value[userId]
                    matrixClient?.closeSuspending()

                    settings.delete(userId)
                    matrixClients.update { it - userId }
                    secretByteArrays.deleteDatabaseKey(userId)
                    deleteAccountData(userId)
                    _initFromStoreResult.value = _initFromStoreResult.value?.remove(userId)
                }
            }
            .onFailure { log.warn(it) { "failed to remove user data fro $userId" } }

    override fun close() {
        value.values.forEach { it.close() }
    }
}

@Serializable
sealed interface MatrixClientInitializationException {
    @Serializable
    data class DatabaseAccessException(override val message: String? = null) :
        MatrixClientInitializationException, RuntimeException(message)

    @Serializable
    data class DatabaseLockedException(override val message: String? = null) :
        MatrixClientInitializationException, RuntimeException(message)

    @Serializable
    data class Unknown(override val message: String? = null) :
        MatrixClientInitializationException, RuntimeException(message)
}

private fun InitFromStoreResult.remove(id: UserId) = copy(success = success.minus(id), failures = failures.minus(id))
