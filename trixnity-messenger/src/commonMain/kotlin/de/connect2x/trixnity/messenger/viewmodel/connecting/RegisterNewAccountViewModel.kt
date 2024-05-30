package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.HttpClientFactory
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterNewAccountViewModel.RegistrationState
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.authentication.AccountType
import net.folivo.trixnity.clientserverapi.model.authentication.Register
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationRequest
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface RegisterNewAccountViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        serverUrl: String,
        onLogin: () -> Unit,
        onBack: () -> Unit,
    ): RegisterNewAccountViewModel =
        RegisterNewAccountViewModelImpl(viewModelContext, serverUrl, onLogin, onBack)

    companion object : RegisterNewAccountViewModelFactory
}

interface RegisterNewAccountViewModel {
    val isFirstMatrixClient: StateFlow<Boolean?>

    val error: StateFlow<String?>
    val registrationState: StateFlow<RegistrationState>

    val registrationOptions: StateFlow<List<AuthenticationType>>
    val loadingRegistrationOptions: StateFlow<Boolean>
    val selectedRegistration: MutableStateFlow<AuthenticationType?>

    val serverUrl: String

    val username: MutableStateFlow<String>
    val password: MutableStateFlow<String>

    val registrationToken: MutableStateFlow<String>

    val canRegisterNewUser: StateFlow<Boolean>
    val addMatrixAccountState: StateFlow<AddMatrixAccountState>

    fun tryRegistration()
    fun back()

    /**
     * Part of [tryRegistration] and thus should not be used directly. Should only be used to override the login process.
     */
    suspend fun loginWithAccessToken(userId: UserId, deviceId: String, accessToken: String)

    sealed interface RegistrationState {
        object Initial : RegistrationState
        object Registering : RegistrationState
        data class Error(val message: String) : RegistrationState
    }
}

// TODO this should re-use an common UIA implementation, that we also need in other components
class RegisterNewAccountViewModelImpl(
    viewModelContext: ViewModelContext,
    override val serverUrl: String,
    private val onLogin: () -> Unit,
    private val onBack: () -> Unit,
) : RegisterNewAccountViewModel, ViewModelContext by viewModelContext {

    private val httpClientFactory = get<HttpClientFactory>()()
    private val getDefaultDeviceDisplayName = get<GetDefaultDeviceDisplayName>()
    override val isFirstMatrixClient: StateFlow<Boolean?> = matrixClients.map { it.isEmpty() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val registrationState: MutableStateFlow<RegistrationState> = MutableStateFlow(RegistrationState.Initial)
    override val registrationOptions: MutableStateFlow<List<AuthenticationType>> = MutableStateFlow(emptyList())
    override val loadingRegistrationOptions: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val selectedRegistration: MutableStateFlow<AuthenticationType?> = MutableStateFlow(null)

    override val username: MutableStateFlow<String> = MutableStateFlow("")
    override val password: MutableStateFlow<String> = MutableStateFlow("")

    override val registrationToken: MutableStateFlow<String> = MutableStateFlow("")
    override val addMatrixAccountState: MutableStateFlow<AddMatrixAccountState> =
        MutableStateFlow(AddMatrixAccountState.None)

    override val canRegisterNewUser: StateFlow<Boolean> = combine(
        username, password, selectedRegistration, registrationToken
    ) { username, password, selectedRegistration, registrationToken ->
        log.debug { "canRegisterNewUser: username=$username, selectedRegistration=$selectedRegistration, registrationToken=$registrationToken" }
        when {
            username.isBlank() -> false
            password.isBlank() -> false
            selectedRegistration == AuthenticationType.RegistrationToken && registrationToken.isBlank() -> false
            else -> true
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, false) // is used down below

    init {
        coroutineScope.launch {
            try {
                val api =
                    MatrixClientServerApiClientImpl(
                        Url(serverUrl),
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

    override fun tryRegistration() {
        val canRegisterNewUser = canRegisterNewUser.value
        log.info { "try registration (canRegisterNewUser = $canRegisterNewUser, username = ${username.value}, password = *******)" }
        coroutineScope.launch {
            registrationState.update { RegistrationState.Registering }
            if (selectedRegistration.value is AuthenticationType.RegistrationToken && canRegisterNewUser) {
                log.info { "registration with token: ${registrationToken.value}" }
                try {
                    val api =
                        MatrixClientServerApiClientImpl(Url(serverUrl), httpClientFactory = httpClientFactory)
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
                                initialDeviceDisplayName = getDefaultDeviceDisplayName()
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
                                        loginWithAccessToken(registerResponse.userId, deviceId, accessToken)
                                    } else {
                                        log.error { "accessToken or deviceId missing in registration response" }
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

    override suspend fun loginWithAccessToken(
        userId: UserId,
        deviceId: String,
        accessToken: String,
    ) {
        matrixClients.loginWithCatching(
            baseUrl = serverUrl,
            loginInfo = MatrixClient.LoginInfo(
                userId = userId,
                deviceId = deviceId,
                accessToken = accessToken,
            ),
            addMatrixAccountState = addMatrixAccountState,
            i18n = i18n,
            onLogin = onLogin,
        )
    }

    override fun back() {
        onBack()
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

    private tailrec suspend fun doFlows(uia: UIA<Register.Response>): Register.Response? {
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
    override val isFirstMatrixClient: StateFlow<Boolean?> = MutableStateFlow(false)
    override val serverUrl: String = "http://localhost:8008"
    override val username: MutableStateFlow<String> = MutableStateFlow("user1")
    override val password: MutableStateFlow<String> = MutableStateFlow("user1-password")
    override val registrationToken: MutableStateFlow<String> = MutableStateFlow("myRegistrationToken")
    override val addMatrixAccountState: MutableStateFlow<AddMatrixAccountState> =
        MutableStateFlow(AddMatrixAccountState.None)
    override val canRegisterNewUser: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override fun tryRegistration() {}
    override fun back() {}
    override suspend fun loginWithAccessToken(userId: UserId, deviceId: String, accessToken: String) {}
}
