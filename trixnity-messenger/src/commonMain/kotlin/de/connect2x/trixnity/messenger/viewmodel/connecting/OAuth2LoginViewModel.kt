package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.util.UriCaller
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2LoginViewModel.State
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import de.connect2x.trixnity.clientserverapi.client.MatrixClientAuthProviderData
import de.connect2x.trixnity.clientserverapi.client.oauth2.ApplicationType
import de.connect2x.trixnity.clientserverapi.client.oauth2.LocalizedField
import de.connect2x.trixnity.clientserverapi.client.oauth2.OAuth2LoginFlow
import de.connect2x.trixnity.clientserverapi.client.oauth2.oAuth2Login
import de.connect2x.trixnity.clientserverapi.model.authentication.oauth2.PromptValue
import de.connect2x.trixnity.clientserverapi.model.authentication.oauth2.ResponseMode
import org.koin.core.component.get
import org.koin.core.component.inject
import kotlin.concurrent.atomics.AtomicReference
import kotlin.reflect.KClass

interface OAuth2LoginViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        type: OAuth2LoginViewModel.Type,
        serverUrl: String,
        initialState: OAuth2LoginFlow.AuthRequestData.State?,
        onLogin: () -> Unit,
        onBack: () -> Unit,
    ): OAuth2LoginViewModel {
        return OAuth2LoginViewModelImpl(
            viewModelContext = viewModelContext,
            type = type,
            serverUrl = serverUrl,
            initialState = initialState,
            onLogin = onLogin,
            onBack = onBack,
        )
    }

    companion object : OAuth2LoginViewModelFactory
}

interface OAuth2LoginViewModel {
    val isFirstMatrixClient: StateFlow<Boolean?>
    val serverUrl: String
    val type: Type

    val state: StateFlow<State>

    /**
     * Opens OAuth2 provider login page, waits to receive the token and logs in with this token.
     */
    fun startLogin()

    /**
     * Resume a previously started OAuth2 flow with a given redirect URL
     */
    fun resumeLogin(callbackUri: String)
    fun abortLogin()

    fun back()

    @Serializable
    enum class Type {
        LOGIN,
        REGISTER
    }

    sealed interface State {
        data object None : State
        data object StartLogin : State
        data object WaitingForRedirect : State
        data object ResumeLogin : State
        data object Success : State
        data class Failure(val message: String) : State
    }
}


open class OAuth2LoginViewModelImpl(
    viewModelContext: ViewModelContext,
    override val type: OAuth2LoginViewModel.Type,
    override val serverUrl: String,
    private val initialState: OAuth2LoginFlow.AuthRequestData.State?,
    private val onLogin: () -> Unit,
    private val onBack: () -> Unit,
) : ViewModelContext by viewModelContext, OAuth2LoginViewModel {
    override val isFirstMatrixClient: StateFlow<Boolean?> = matrixClients.map { it.isEmpty() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val config = get<MatrixMessengerConfiguration>()
    private val getDefaultDeviceDisplayName by inject<GetDefaultDeviceDisplayName>()

    private val flow: MutableStateFlow<OAuth2LoginFlow> = MutableStateFlow(makeLoginFlow())
    private fun makeLoginFlow(
        loginHint: String? = null,
        promptValue: PromptValue? = null
    ) = MatrixClientAuthProviderData.oAuth2Login(
        baseUrl = Url(serverUrl),
        applicationType = platformApplicationType,
        clientUri = config.oAuth2ClientUrl,
        redirectUri = "${config.appUri}/${config.appUriOAuth2Redirect}",
        responseMode = ResponseMode.Fragment,
        clientName = LocalizedField(getDefaultDeviceDisplayName()),
        promptValue = when {
            promptValue != null -> promptValue
            type == OAuth2LoginViewModel.Type.REGISTER -> PromptValue.Create
            else -> null
        },
        initialState = initialState,
        httpClientEngine = config.httpClientEngine,
        httpClientConfig = config.httpClientConfig,
        loginHint = loginHint,
    )

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val uriCaller = get<UriCaller>()

    override val state: MutableStateFlow<State> = MutableStateFlow(State.None)

    override fun startLogin() {
        if (checkState(State.None::class, requestedState = State.StartLogin)) {
            coroutineScope.launch {
                log.debug { "start login" }
                flow.updateAndGet { makeLoginFlow(loginHint.load(), promptValue.load()) }.createAuthRequest()
                    .onSuccess { authRequestData ->
                        log.debug { "createAuthRequest successful" }
                        messengerSettings.update<MatrixMessengerSettingsBase> {
                            it.copy(
                                oAuth2LoginState = MatrixMessengerSettingsBase.OAuth2LoginState(
                                    serverUrl = serverUrl,
                                    type = type,
                                    state = authRequestData.state,
                                )
                            )
                        }
                        log.debug { "Redirecting to ${authRequestData.url}" }
                        try {
                            uriCaller(authRequestData.url, false)
                            state.value = State.WaitingForRedirect
                        } catch (exception: Exception) {
                            log.warn(exception) { "could not open uri" }
                            state.value = State.Failure(i18n.uriCallFailure())
                        }
                    }.onFailure { exception ->
                        log.warn(exception) { "createAuthRequest failed" }
                        state.value = State.Failure(i18n.oAuth2LoginFailure(exception.message))
                    }
            }
        }
    }

    override fun resumeLogin(callbackUri: String) {
        if (checkState(State.None::class, State.WaitingForRedirect::class, requestedState = State.ResumeLogin)) {
            coroutineScope.launch {
                log.debug { "resume login" }
                messengerSettings.update<MatrixMessengerSettingsBase> {
                    it.copy(oAuth2LoginState = null)
                }
                flow.value.onCallback(Url(callbackUri))
                    .onFailure { exception ->
                        log.warn(exception) { "onCallback failed" }
                        state.value = State.Failure(i18n.oAuth2LoginFailure(exception.message))
                    }
                    .onSuccess { authProviderData ->
                        log.debug { "onCallback successful" }
                        when (val createMatrixClientResult = matrixClients.create(authProviderData)) {
                            is MatrixClients.CreateResult.Success -> {
                                state.value = State.Success
                                onLogin()
                            }

                            is MatrixClients.CreateResult.Failure -> {
                                state.value = State.Failure(createMatrixClientResult.message)
                            }
                        }
                    }
            }
        }
    }

    override fun abortLogin() {
        coroutineScope.launch {
            log.debug { "abort login" }
            state.value = State.None
            messengerSettings.update<MatrixMessengerSettingsBase> { it.copy(ssoLoginState = null) }
        }
    }

    override fun back() {
        abortLogin()
        onBack()
    }

    val backCallback = BackCallback {
        back()
    }

    private val loginHint: AtomicReference<String?> = AtomicReference(null)

    @TrixnityMessengerPrivateApi
    fun setLoginHint(hint: String?) {
        loginHint.store(hint)
    }

    private val promptValue: AtomicReference<PromptValue?> = AtomicReference(null)

    @TrixnityMessengerPrivateApi
    fun setPromptValue(value: PromptValue?) {
        promptValue.store(value)
    }

    init {
        registerBackCallback(backCallback)
    }

    private fun checkState(
        vararg requiredState: KClass<out State>,
        requestedState: State
    ): Boolean =
        state.updateAndGet { currentState ->
            if (requiredState.any { it.isInstance(currentState) }) requestedState
            else currentState
        } == requestedState
}

expect val platformApplicationType: ApplicationType

class PreviewOAuth2LoginViewModel : OAuth2LoginViewModel {
    override val serverUrl: String = "https://timmy-messenger.de"
    override val type: OAuth2LoginViewModel.Type = OAuth2LoginViewModel.Type.LOGIN
    override val state: StateFlow<State> = MutableStateFlow(State.None)
    override val isFirstMatrixClient: StateFlow<Boolean?> = MutableStateFlow(false)
    override fun startLogin() {}
    override fun resumeLogin(callbackUri: String) {}
    override fun abortLogin() {}
    override fun back() {}
}
