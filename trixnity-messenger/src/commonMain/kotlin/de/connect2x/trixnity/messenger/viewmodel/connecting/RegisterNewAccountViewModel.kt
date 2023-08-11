package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.GetAccountNames
import de.connect2x.trixnity.messenger.MatrixClientService
import de.connect2x.trixnity.messenger.deviceDisplayName
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel.LoginState
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterNewAccountViewModel.RegistrationState
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.combine
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.http.*
import korlibs.io.async.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.serverDiscovery
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.authentication.AccountType
import net.folivo.trixnity.clientserverapi.model.authentication.Register
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationRequest
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import org.koin.core.component.get
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

interface RegisterNewAccountViewModelFactory {
    fun newRegisterNewAccountViewModel(
        viewModelContext: ViewModelContext,
        matrixClientService: MatrixClientService,
        onLogin: () -> Unit,
        onCancel: () -> Unit,
    ): RegisterNewAccountViewModel =
        RegisterNewAccountViewModelImpl(viewModelContext, matrixClientService, onLogin, onCancel)
}

interface RegisterNewAccountViewModel {
    val error: StateFlow<String?>
    val registrationState: StateFlow<RegistrationState>

    val registrationOptions: StateFlow<List<AuthenticationType>>
    val loadingRegistrationOptions: StateFlow<Boolean>
    val selectedRegistration: MutableStateFlow<AuthenticationType?>

    val existingAccountNames: StateFlow<List<String>?>
    val accountName: MutableStateFlow<String>
    val serverUrl: MutableStateFlow<String>
    val username: MutableStateFlow<String>
    val password: MutableStateFlow<String>
    val displayName: MutableStateFlow<String?>
    val serverUrlValidation: StateFlow<ServerUrlValidation>

    val registrationToken: MutableStateFlow<String>

    val canRegisterNewUser: StateFlow<Boolean>
    val loginState: StateFlow<LoginState>

    fun tryRegistration()
    fun cancel()

    sealed interface RegistrationState {
        object Initial : RegistrationState
        object Registering : RegistrationState
        data class Error(val message: String) : RegistrationState
    }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
open class RegisterNewAccountViewModelImpl(
    viewModelContext: ViewModelContext,
    private val matrixClientService: MatrixClientService,
    private val onLogin: () -> Unit,
    private val onCancel: () -> Unit,
    private val httpClientFactory: (HttpClientConfig<*>.() -> Unit) -> HttpClient = { HttpClient(it) },
) : RegisterNewAccountViewModel, ViewModelContext by viewModelContext {

    private val getAccountNames = get<GetAccountNames>()

    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val registrationState: MutableStateFlow<RegistrationState> = MutableStateFlow(RegistrationState.Initial)
    override val registrationOptions: MutableStateFlow<List<AuthenticationType>> = MutableStateFlow(emptyList())
    override val loadingRegistrationOptions: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val selectedRegistration: MutableStateFlow<AuthenticationType?> = MutableStateFlow(null)

    override val existingAccountNames: StateFlow<List<String>?> = channelFlow { send(getAccountNames()) }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val accountName: MutableStateFlow<String> = MutableStateFlow("Standard")
    override val serverUrl: MutableStateFlow<String> = MutableStateFlow("")
    override val username: MutableStateFlow<String> = MutableStateFlow("")
    override val displayName: MutableStateFlow<String?> = MutableStateFlow(null)
    override val password: MutableStateFlow<String> = MutableStateFlow("")

    override val registrationToken: MutableStateFlow<String> = MutableStateFlow("")
    override val loginState: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.Initial)

    override val canRegisterNewUser: StateFlow<Boolean> = combine(
        accountName, existingAccountNames, username, password, selectedRegistration, registrationToken
    ) { accountName, existingAccountNames, username, password, selectedRegistration, registrationToken ->
        when {
            existingAccountNames?.contains(accountName) != false -> false
            username.isBlank() -> false
            password.isBlank() -> false
            selectedRegistration == AuthenticationType.RegistrationToken && registrationToken.isBlank() -> false
            else -> true
        }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val serverUrlValidation: StateFlow<ServerUrlValidation> = serverUrl
        .debounce(1.seconds)
        .transformLatest { serverUrl ->
            log.debug { "serverUrl: $serverUrl" }
            when {
                serverUrl.isBlank() -> emit(ServerUrlValidation.None)

                serverUrl.startsWith("localhost") || serverUrl.startsWith("http://localhost") ||
                        serverUrl.startsWith("http://docker:") -> // for ITs in CI
                    try {
                        emit(ServerUrlValidation.Valid(Url(serverUrl)))
                    } catch (exc: Exception) {
                        emit(ServerUrlValidation.None)
                    }

                else -> {
                    emit(ServerUrlValidation.Started)
                    coroutineScope {
                        val minDelay = launch { delay(500.milliseconds) }
                        val result = withTimeoutOrNull(3.seconds) {
                            serverUrl.serverDiscovery(httpClientFactory).fold(
                                onSuccess = {
                                    log.debug { "server url can be determined" }
                                    ServerUrlValidation.Valid(it)
                                },
                                onFailure = {
                                    log.debug { "server url cannot be determined" }
                                    ServerUrlValidation.Invalid(i18n.serverDiscoveryFailed())
                                })
                        }
                        minDelay.join()
                        if (result == null) emit(ServerUrlValidation.Invalid(i18n.serverDiscoveryFailed()))
                        else emit(result)
                    }
                }
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), ServerUrlValidation.None)

    init {
        coroutineScope.launch {
            serverUrlValidation.collect { serverUrlValidation ->
                if (serverUrlValidation is ServerUrlValidation.Valid) {
                    try {
                        val api =
                            MatrixClientServerApiClientImpl(
                                serverUrlValidation.url,
                                httpClientFactory = httpClientFactory
                            )
                        log.info { "serverUrl: $serverUrl" }
                        loadingRegistrationOptions.update { true }
                        api.authentication.register(accountType = AccountType.USER)
                            .onFailure {
                                log.error(it) { "cannot initiate UIA" }
                                registrationOptions.update { emptyList() }
                                error.value = i18n.registrationErrorNotSupported()
                            }
                            .onSuccess {
                                log.debug { "initiate flow" }
                                determineRegistrationFlows(it)
                            }
                    } catch (exc: Exception) {
                        log.warn(exc) { "try to initiate register UIA" }
                        registrationOptions.update { emptyList() }
                        selectedRegistration.update { null }
                    } finally {
                        loadingRegistrationOptions.update { false }
                    }
                }
            }
        }
    }

    override fun tryRegistration() {
        val urlValidation = serverUrlValidation.value
        log.info { "try registration (serverUrl: $urlValidation, accountName = ${accountName.value}, username = ${username.value}, password = *******)" }
        coroutineScope.launch {
            registrationState.update { RegistrationState.Registering }
            if (selectedRegistration.value is AuthenticationType.RegistrationToken && urlValidation is ServerUrlValidation.Valid) {
                log.info { "registration with token: ${registrationToken.value}" }
                try {
                    val api =
                        MatrixClientServerApiClientImpl(urlValidation.url, httpClientFactory = httpClientFactory)
                    api.authentication.isRegistrationTokenValid(registrationToken.value)
                        .onFailure {
                            log.error(it) { "registration token ${registrationToken.value} is not valid" }
                            registrationState.update { RegistrationState.Error(i18n.registrationTokenNotValid()) }
                        }
                        .onSuccess {
                            api.authentication.register(
                                accountType = AccountType.USER,
                                username = username.value,
                                password = password.value,
                                initialDeviceDisplayName = deviceDisplayName()
                            )
                                .onFailure { exc ->
                                    log.error(exc) { "cannot initiate UIA" }
                                    if (exc is MatrixServerException) {
                                        registrationState.update {
                                            RegistrationState.Error(
                                                when (exc.errorResponse) {
                                                    is ErrorResponse.UserInUse -> i18n.registrationErrorUserInUse()
                                                    is ErrorResponse.InvalidUsername -> i18n.registrationErrorInvalidUsername()
                                                    is ErrorResponse.Exclusive -> i18n.registrationErrorInvalidUsername() // for users this is the same
                                                    else -> i18n.registrationErrorNotSupported()
                                                }
                                            )
                                        }
                                    } else {
                                        registrationState.update { RegistrationState.Error(i18n.registrationErrorNotSupported()) }
                                    }
                                }
                                .onSuccess {
                                    log.info { "try to do UIA to retrieve access_token" }
                                    val registerResponse = doFlows(it)
                                    val deviceId = registerResponse?.deviceId
                                    val accessToken = registerResponse?.accessToken
                                    if (deviceId != null && accessToken != null) {
                                        loginWith(
                                            matrixClientService = matrixClientService,
                                            accountName = accountName.value,
                                            serverUrl = urlValidation.url.toString(),
                                            userId = registerResponse.userId,
                                            deviceId = deviceId,
                                            accessToken = accessToken,
                                            displayName = displayName.value ?: username.value,
                                            avatarUrl = null,
                                            loginState = loginState,
                                            onLogin = onLogin,
                                        )
                                    } else {
                                        log.error { "access token cannot be accessed" }
                                        registrationState.update { RegistrationState.Error(i18n.registrationErrorNotSuccessful()) }
                                    }
                                }
                        }
                } finally {
                    if (registrationState.value !is RegistrationState.Error) {
                        registrationState.update { RegistrationState.Initial }
                    }
                }
            }
        }
    }

    override fun cancel() {
        onCancel()
    }

    private fun determineRegistrationFlows(uia: UIA<Register.Response>) {
        when (uia) {
            is UIA.Success -> {
                log.warn { "should not happen" }
            }

            is UIA.Step -> {
                val flows = uia.state.flows
                log.debug { "UIA flows: $flows" }
                registrationOptions.update {
                    flows
                        .filter { it.stages.isNotEmpty() && it.stages.first() != AuthenticationType.Dummy }
                        .map { it.stages.first() }
                }
                selectedRegistration.update { registrationOptions.value.first() }
            }

            is UIA.Error -> {
                log.error { "UIA flow in error state: ${uia.errorResponse.error}" }
                selectedRegistration.update { null }
                error.value = i18n.registrationErrorCannotDetermine()
            }
        }
    }

    private suspend fun doFlows(uia: UIA<Register.Response>): Register.Response? {
        log.info { uia }
        when (uia) {
            is UIA.Success -> {
                log.info { "successfully authenticated" }
                return uia.value
            }

            is UIA.Step -> {
                val flows = uia.state.flows
                log.debug { "UIA flows: $flows" }
                val completedStages = uia.state.completed
                // TODO at the moment only support flows with registration token
                flows.firstOrNull { flow ->
                    flow.stages
                        .filterNot { completedStages.contains(it) }
                        .all { it == AuthenticationType.RegistrationToken || it == AuthenticationType.Dummy }
                }?.let { flow ->
                    flow.stages.filterNot { completedStages.contains(it) }.firstOrNull()?.let { firstStage ->
                        log.debug { "first stage is $firstStage, token is ${registrationToken.value}" }
                        when (firstStage) {
                            is AuthenticationType.RegistrationToken -> {
                                uia.authenticate(AuthenticationRequest.RegistrationToken(registrationToken.value))
                                    .onFailure {
                                        log.error(it) { "cannot use registration token" }
                                        registrationState.update { RegistrationState.Error(i18n.registrationErrorNotSuccessful()) }
                                    }
                                    .onSuccess { response -> return doFlows(response) }
                            }

                            is AuthenticationType.Dummy -> {
                                uia.authenticate(AuthenticationRequest.Dummy)
                                    .onFailure {
                                        log.error(it) { "cannot perform dummy authentication" }
                                        registrationState.update { RegistrationState.Error(i18n.registrationErrorNotSuccessful()) }
                                    }
                                    .onSuccess { response -> return doFlows(response) }
                            }

                            else -> {
                                log.error { "unexpected registration stage -> do nothing" }
                            }
                        }
                    }
                }
                    ?: log.warn { "no suitable registration flow found (only registration token is supported at the moment)" }
            }

            is UIA.Error -> {
                log.error { "UIA flow in error state: ${uia.errorResponse.error}" }
                registrationState.update { RegistrationState.Error(i18n.registrationErrorCannotDetermine()) }
            }
        }

        return null
    }
}

class PreviewRegisterNewAccountViewModel : RegisterNewAccountViewModel {
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val registrationState: MutableStateFlow<RegistrationState> = MutableStateFlow(RegistrationState.Initial)
    override val registrationOptions: MutableStateFlow<List<AuthenticationType>> = MutableStateFlow(
        listOf(
            AuthenticationType.RegistrationToken,
            AuthenticationType.Password,
        )
    )
    override val loadingRegistrationOptions: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val selectedRegistration: MutableStateFlow<AuthenticationType?> =
        MutableStateFlow(AuthenticationType.RegistrationToken)
    override val existingAccountNames: MutableStateFlow<List<String>?> = MutableStateFlow(emptyList())
    override val accountName: MutableStateFlow<String> = MutableStateFlow("Standard")
    override val serverUrl: MutableStateFlow<String> = MutableStateFlow("http://localhost:8008")
    override val username: MutableStateFlow<String> = MutableStateFlow("user1")
    override val password: MutableStateFlow<String> = MutableStateFlow("user1-password")
    override val displayName: MutableStateFlow<String?> = MutableStateFlow(null)
    override val serverUrlValidation: MutableStateFlow<ServerUrlValidation> =
        MutableStateFlow(ServerUrlValidation.Valid(Url("http://localhost:8008")))
    override val registrationToken: MutableStateFlow<String> = MutableStateFlow("myRegistrationToken")
    override val loginState: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.Initial)
    override val canRegisterNewUser: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override fun tryRegistration() {}
    override fun cancel() {}

}
