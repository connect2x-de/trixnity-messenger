package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.MatrixClientService
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel.LoginState
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import korlibs.io.async.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.authentication.AccountType
import net.folivo.trixnity.clientserverapi.model.authentication.Register
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationRequest
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType

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

    val registrationOptions: StateFlow<List<AuthenticationType>?>
    val selectedRegistration: StateFlow<AuthenticationType?>

    val accountName: MutableStateFlow<String>
    val serverUrl: MutableStateFlow<String>
    val username: MutableStateFlow<String>
    val password: MutableStateFlow<String>

    val registrationInProgress: StateFlow<Boolean>

    val needsRegistrationToken: StateFlow<Boolean>
    val registrationToken: MutableStateFlow<String>

    val loginState: StateFlow<LoginState>

    fun tryRegistration()
    fun cancel()
}

class RegisterNewAccountViewModelImpl(
    viewModelContext: ViewModelContext,
    private val matrixClientService: MatrixClientService,
    private val onLogin: () -> Unit,
    private val onCancel: () -> Unit,
) : RegisterNewAccountViewModel, ViewModelContext by viewModelContext {

    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val registrationOptions: MutableStateFlow<List<AuthenticationType>?> = MutableStateFlow(null)
    override val selectedRegistration: MutableStateFlow<AuthenticationType?> = MutableStateFlow(null)

    override val accountName: MutableStateFlow<String> = MutableStateFlow("")
    override val serverUrl: MutableStateFlow<String> = MutableStateFlow("")
    override val username: MutableStateFlow<String> = MutableStateFlow("")
    override val password: MutableStateFlow<String> = MutableStateFlow("")

    override val registrationInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val needsRegistrationToken: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val registrationToken: MutableStateFlow<String> = MutableStateFlow("")
    override val loginState: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.Initial)

    init {
        coroutineScope.launch {
            serverUrl.collect { serverUrl ->
                if (serverUrl.isNotBlank()) {
                    try {
                        val api = MatrixClientServerApiClientImpl(Url(serverUrl))
                        log.info { "serverUrl: $serverUrl" }
                        api.authentication.register(accountType = AccountType.USER)
                            .onFailure {
                                log.error(it) { "cannot initiate UIA" }
                                error.value = i18n.registrationNotSupported()
                            }
                            .onSuccess {
                                log.debug { "initiate flow"}
                                determineRegistrationFlows(it)
                            }
                    } catch (exc: URLParserException) {
                        // do nothing
                    }
                }
            }
        }

        coroutineScope.launch {
            selectedRegistration.collect { authenticationType ->
                when (authenticationType) {
                    is AuthenticationType.RegistrationToken -> {
                        needsRegistrationToken.update { true }
                    }

                    else -> {
                        log.warn { "Only registration token is supported at the moment." }
                        error.value = i18n.registrationNotSupported()
                    }
                }
            }
        }
    }

    override fun tryRegistration() {
        coroutineScope.launch {
            registrationInProgress.update { true }
            if (selectedRegistration.value is AuthenticationType.RegistrationToken) {
                try {
                    val api = MatrixClientServerApiClientImpl(Url(serverUrl.value))
                    api.authentication.register(
                        accountType = AccountType.USER,
                        username = username.value,
                        password = password.value,
                    )
                        .onFailure {
                            log.error(it) { "cannot initiate UIA" }
                            error.value = i18n.registrationNotSupported()
                        }
                        .onSuccess {
                            log.info { "try to do UIA to retrieve access_token" }
                            val accessToken = doFlows(it)
                            if (accessToken != null) {
                                login(
                                    matrixClientService,
                                    accountName.value,
                                    serverUrl.value,
                                    username.value,
                                    password.value,
                                    loginState,
                                    onLogin,
                                )
                            } else {
                                log.error { "access token cannot be accessed" }
                                error.value = i18n.registrationNotSuccessful()
                            }
                        }
                } finally {
                    registrationInProgress.update { false }
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
                registrationOptions.update { flows.filter { it.stages.isNotEmpty() }.map { it.stages.first() } }
                selectedRegistration.update { registrationOptions.value?.first() }
            }

            is UIA.Error -> {
                log.error { "UIA flow in error state: ${uia.errorResponse.error}" }
                error.value = i18n.registrationCannotDetermine()
            }
        }
    }

    private suspend fun doFlows(uia: UIA<Register.Response>): String? {
        log.info { uia }
        when (uia) {
            is UIA.Success -> {
                log.info { "successfully authenticated" }
                return uia.value.accessToken
            }

            is UIA.Step -> {
                val flows = uia.state.flows
                log.debug { "UIA flows: $flows" }
                // TODO at the moment only support 1 flow with multiple stages
                if (flows.size > 1) {
                    log.warn { "Only a single flow is supported at the moment" }
                } else {
                    val completedStages = uia.state.completed
                    flows.firstOrNull()?.let { flow ->
                        flow.stages.filterNot { completedStages.contains(it) }.firstOrNull()?.let { firstStage ->
                            log.debug { "first stage is $firstStage, token is ${registrationToken.value}" }
                            when (firstStage) {
                                is AuthenticationType.RegistrationToken -> {
                                    uia.authenticate(AuthenticationRequest.RegistrationToken(registrationToken.value))
                                        .onFailure {
                                            log.error(it) { "cannot use registration token" }
                                            error.value = i18n.registrationNotSuccessful()
                                        }
                                        .onSuccess { response ->return doFlows(response) }
                                }

                                is AuthenticationType.Dummy -> {
                                    uia.authenticate(AuthenticationRequest.Dummy)
                                        .onFailure {
                                            log.error(it) { "cannot perform dummy authentication" }
                                            error.value = i18n.registrationNotSuccessful()
                                        }
                                        .onSuccess { response -> return doFlows(response) }
                                }

                                else -> {
                                    log.error { "" }
                                }
                            }
                        }
                    }
                }
            }

            is UIA.Error -> {
                log.error { "UIA flow in error state: ${uia.errorResponse.error}" }
                error.value = i18n.registrationCannotDetermine()
            }
        }

        return null
    }

}
