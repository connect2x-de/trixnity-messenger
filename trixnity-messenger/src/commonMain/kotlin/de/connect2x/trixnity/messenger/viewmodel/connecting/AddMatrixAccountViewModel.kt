package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.*
import de.connect2x.trixnity.messenger.util.I18n
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel.*
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.combine
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.util.network.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.serverDiscovery
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


private val log = KotlinLogging.logger {}

interface AddMatrixAccountViewModelFactory {
    fun newAddMatrixAccountViewModel(
        viewModelContext: ViewModelContext,
        matrixClientService: MatrixClientService,
        onLogin: () -> Unit,
        onCancel: () -> Unit,
    ): AddMatrixAccountViewModel {
        return AddMatrixAccountViewModelImpl(
            viewModelContext,
            matrixClientService,
            onLogin,
            onCancel,
        )
    }
}

interface AddMatrixAccountViewModel {
    val isFirstMatrixClient: StateFlow<Boolean?>
    val canLogin: StateFlow<Boolean>

    enum class Mode {
        USERID, USERNAME
    }

    /**
     * Switch between login modes (login via [Mode.USERID] or [Mode.USERNAME] possible)
     */
    val mode: MutableStateFlow<Mode>

    sealed interface UserIdValidation {
        object None : UserIdValidation

        data class WrongUserId(val message: String) : UserIdValidation
        object ServerDiscoveryStarted : UserIdValidation
        data class ServerDiscoverySuccess(val value: String) : UserIdValidation
        data class ServerDiscoveryFailure(val message: String) : UserIdValidation
    }

    val accountName: MutableStateFlow<String>

    val userId: MutableStateFlow<String>
    val userIdValidation: StateFlow<UserIdValidation>

    val username: MutableStateFlow<String>

    val serverUrl: MutableStateFlow<String>
    val password: MutableStateFlow<String>

    sealed interface LoginState {
        object Initial : LoginState
        object Connecting : LoginState
        object Success : LoginState
        data class Failure(val message: String) : LoginState
    }

    val loginState: StateFlow<LoginState>
    fun tryLogin()
    fun cancel()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
open class AddMatrixAccountViewModelImpl(
    viewModelContext: ViewModelContext,
    private val matrixClientService: MatrixClientService,
    private val onLogin: () -> Unit,
    private val onCancel: () -> Unit,
) : ViewModelContext by viewModelContext, AddMatrixAccountViewModel {

    final override val mode: MutableStateFlow<Mode> = MutableStateFlow(Mode.USERID)

    private val accountNames = channelFlow { send(get<GetAccountNames>()()) }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    override val isFirstMatrixClient: StateFlow<Boolean?> = accountNames.map { it?.isEmpty() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val accountName: MutableStateFlow<String> = MutableStateFlow("Standard")
    final override val userId: MutableStateFlow<String> = MutableStateFlow("")
    final override val username: MutableStateFlow<String> = MutableStateFlow("")
    final override val serverUrl = MutableStateFlow("")
    final override val password: MutableStateFlow<String> = MutableStateFlow("")

    override val loginState: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.Initial)

    final override val userIdValidation =
        combine(
            userId.dropWhile { it.isEmpty() },
            mode,
        ) { userId, mode -> UserId(userId) to mode }
            .debounce(1.seconds)
            .transformLatest { (userId, mode) ->
                val isNoMatrixUserId = isNoMatrixUserId(userId, i18n)
                when {
                    mode == Mode.USERNAME -> emit(UserIdValidation.None)
                    isNoMatrixUserId != null -> emit(UserIdValidation.WrongUserId(isNoMatrixUserId))

                    else -> {
                        emit(UserIdValidation.ServerDiscoveryStarted)
                        coroutineScope {
                            val minDelay = launch { delay(500.milliseconds) }
                            val result = withTimeoutOrNull(3.seconds) {
                                userId.serverDiscovery().fold(
                                    onSuccess = {
                                        log.debug { "server url can be determined" }
                                        UserIdValidation.ServerDiscoverySuccess(it.toString())
                                    },
                                    onFailure = {
                                        log.debug { "server url cannot be determined" }
                                        UserIdValidation.ServerDiscoveryFailure(i18n.serverDiscoveryFailed())
                                    })
                            }
                            minDelay.join()
                            if (result == null) emit(UserIdValidation.ServerDiscoveryFailure(i18n.serverDiscoveryFailed()))
                            else emit(result)
                        }
                    }
                }
            }.stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(),
                UserIdValidation.None
            )

    override val canLogin: StateFlow<Boolean> =
        combine(
            accountName,
            userId,
            userIdValidation,
            username,
            password,
            serverUrl,
            mode,
        ) { accountName, userId, userIdValidation, username, password, serverUrl, mode ->
            log.trace { "canLogin: accountName=$accountName, userId=$userId, userIdValidation=$userIdValidation, username=$username, serverUrl=$serverUrl, mode=$mode" }
            val accountAlreadyExist = accountNames.value?.contains(accountName) ?: false
            loginState.value = when {
                accountName.isBlank() -> LoginState.Failure(i18n.accountNameMustNotBeEmpty())
                accountAlreadyExist -> LoginState.Failure(i18n.accountAlreadyExistsLocally(accountName))
                else -> LoginState.Initial
            }
            when {
                accountAlreadyExist -> false
                accountName.isBlank() -> false
                mode == Mode.USERID -> userId.isNotBlank() && userIdValidation is UserIdValidation.ServerDiscoverySuccess && password.isNotBlank()
                mode == Mode.USERNAME -> username.isNotBlank() && password.isNotBlank() && serverUrl.isNotBlank()
                else -> true
            }
        }.stateIn(coroutineScope, SharingStarted.Eagerly, false) // eagerly because value is used below

    init {
        coroutineScope.launch {
            userIdValidation.collect {
                if (mode.value == Mode.USERID)
                    serverUrl.value =
                        if (it is UserIdValidation.ServerDiscoverySuccess) it.value
                        else ""
            }
        }
        coroutineScope.launch {
            userId.collect {
                if (mode.value == Mode.USERID)
                    username.value = UserId(it).localpart
            }
        }
        coroutineScope.launch {
            username.collect {
                if (mode.value == Mode.USERNAME)
                    userId.value = ""
            }
        }
    }

    override fun tryLogin() {
        log.debug { "Try to login into ${userIdValidation.value} with username ${userId.value} and password *************." }
        if (canLogin.value && loginState.value !is LoginState.Connecting) {
            loginState.value = LoginState.Connecting
            coroutineScope.launch {
                val errorMessage = try {
                    matrixClientService.login(
                        baseUrl = Url(serverUrl.value),
                        identifier = IdentifierType.User(username.value),
                        password = password.value,
                        initialDeviceDisplayName = deviceDisplayName(),
                        accountName = accountName.value,
                    ).getOrThrow()
                    log.info { "login success" }
                    null
                } catch (exc: MatrixServerException) {
                    log.error(exc) { "Cannot contact Matrix Server." }
                    when (exc.statusCode) {
                        HttpStatusCode.Forbidden -> i18n.connectingErrorForbidden()
                        HttpStatusCode.NotFound -> i18n.connectingErrorNotFound()
                        else -> i18n.connectingErrorStandard()
                    }
                } catch (exc: CancellationException) {
                    // do nothing as this is the case when the view model is removed
                    null
                } catch (exc: StoreLockedException) {
                    log.error(exc) { "database is locked" }
                    i18n.connectingErrorDbLocked()
                } catch (exc: StoreAccessException) {
                    log.error(exc) { "cannot access database; this is a serious problem and might only be solved by deleting the database if the problem persists" }
                    // we cannot load data from the DB, so either close the App or remove the DB and try again
                    i18n.connectingErrorDbAccess()
                } catch (exc: Exception) {
                    log.error(exc) { "Cannot contact Matrix Server." }
                    when (exc) {
                        is UnresolvedAddressException, is IllegalArgumentException ->
                            i18n.connectingErrorWrongAddress()

                        is IOException -> {
                            handleIoException(exc)
                        }

                        else -> handleCause(exc.cause)
                    }
                }

                if (errorMessage == null) {
                    loginState.value = LoginState.Success
                    onLogin()
                } else {
                    loginState.value = LoginState.Failure(errorMessage)
                }
            }
        } else {
            log.warn { "cannot login: canCreateMatrixClient (${canLogin.value}), serverUrl (${serverUrl.value})" }
        }
    }

    // HACK to circumvent https://youtrack.jetbrains.com/issue/KTOR-1372
    private suspend fun handleIoException(exc: Exception): String {
        log.error { exc }
        return if (exc.message == "Connection refused" ||
            exc.message?.startsWith("Failed to connect") == true ||
            exc.message == "Verbindungsaufbau abgelehnt"
        ) {
            i18n.connectingErrorStandard()
        } else if (exc.message?.startsWith("Cleartext HTTP traffic") == true) {
            i18n.connectingErrorHttps()
        } else {
            handleCause(exc.cause)
        }
    }

    private suspend fun handleCause(exc: Throwable?) = if (exc != null) {
        when (exc) {
            is UnresolvedAddressException, is IllegalArgumentException ->
                i18n.connectingErrorWrongAddress()

            is IOException -> {
                handleIoException(exc)
            }

            else -> i18n.connectingErrorStandard()
        }
    } else {
        log.error { exc }
        i18n.connectingErrorStandard()
    }

    override fun cancel() {
        onCancel()
    }

    protected fun isNoMatrixUserId(userId: UserId, i18n: I18n): String? {
        val hasNoDomain = userId.domain.isBlank()
        val isUrlWrong = try {
            Url(userId.domain)
            false
        } catch (_: Error) {
            true
        }
        return when {
            userId.full.startsWith("@").not() -> i18n.userIdShouldStartWithAt()
            hasNoDomain -> i18n.userIdDomainMissing()
            isUrlWrong -> i18n.userIdServerUrlProblems()
            else -> null
        }
    }
}

class PreviewAddMatrixAccountViewModel : AddMatrixAccountViewModel {
    override val isFirstMatrixClient: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val mode: MutableStateFlow<Mode> = MutableStateFlow(Mode.USERID)
    override val accountName: MutableStateFlow<String> = MutableStateFlow("default")
    override val userId: MutableStateFlow<String> = MutableStateFlow("@timmy:imbitbu.de")
    override val username: MutableStateFlow<String> = MutableStateFlow("timmye")
    override val serverUrl: MutableStateFlow<String> = MutableStateFlow("https://imbitbu.de")
    override val password: MutableStateFlow<String> = MutableStateFlow("")
    override val canLogin: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val userIdValidation: MutableStateFlow<UserIdValidation> = MutableStateFlow(UserIdValidation.None)
    override val loginState: StateFlow<LoginState> = MutableStateFlow(LoginState.Initial)

    override fun tryLogin() {
    }

    override fun cancel() {
    }

}