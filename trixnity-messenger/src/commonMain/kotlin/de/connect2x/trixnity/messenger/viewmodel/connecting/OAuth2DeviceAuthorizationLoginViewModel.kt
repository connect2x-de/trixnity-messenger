package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.clientserverapi.client.MatrixClientAuthProviderData
import de.connect2x.trixnity.clientserverapi.client.oauth2.LocalizedField
import de.connect2x.trixnity.clientserverapi.client.oauth2.OAuth2DeviceAuthorizationLoginFlow
import de.connect2x.trixnity.clientserverapi.client.oauth2.oAuth2DeviceAuthorizationLogin
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.util.UriCaller
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2DeviceAuthorizationLoginViewModel.State
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get
import org.koin.core.component.inject

interface OAuth2DeviceAuthorizationLoginViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        serverUrl: String,
        onLogin: () -> Unit,
        onBack: () -> Unit,
    ): OAuth2DeviceAuthorizationLoginViewModel {
        return OAuth2DeviceAuthorizationLoginViewModelImpl(
            viewModelContext = viewModelContext,
            serverUrl = serverUrl,
            onLogin = onLogin,
            onBack = onBack,
        )
    }

    companion object : OAuth2DeviceAuthorizationLoginViewModelFactory
}

interface OAuth2DeviceAuthorizationLoginViewModel : LoginViewModel {
    val state: StateFlow<State>

    fun openLoginPage()

    fun back()

    sealed interface State {
        data object None : State

        data object ObtainCode : State

        data class CheckCode(val userCode: String, val uri: String, val verificationUri: String?) : State

        data object Success : State

        data class Failure(val message: String) : State
    }
}

open class OAuth2DeviceAuthorizationLoginViewModelImpl(
    viewModelContext: ViewModelContext,
    override val serverUrl: String,
    private val onLogin: () -> Unit,
    private val onBack: () -> Unit,
) : ViewModelContext by viewModelContext, OAuth2DeviceAuthorizationLoginViewModel {
    override val isFirstMatrixClient: StateFlow<Boolean?> =
        matrixClients.map { it.isEmpty() }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val config = get<MatrixMessengerConfiguration>()
    private val getDefaultDeviceDisplayName by inject<GetDefaultDeviceDisplayName>()

    private fun makeLoginFlow() =
        MatrixClientAuthProviderData.oAuth2DeviceAuthorizationLogin(
            baseUrl = Url(serverUrl),
            applicationType = platformOAuth2LoginApplicationType,
            clientUri = config.oAuth2ClientUrl,
            redirectUri = "${config.appUri}/${config.appUriOAuth2Redirect}",
            clientName = LocalizedField(getDefaultDeviceDisplayName()),
            httpClientEngine = config.httpClientEngine,
            httpClientConfig = config.httpClientConfig,
        )

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val uriCaller = get<UriCaller>()

    override val state: MutableStateFlow<State> = MutableStateFlow(State.None)
    private val authRequestData = MutableStateFlow<OAuth2DeviceAuthorizationLoginFlow.AuthRequestData?>(null)

    private val backCallback = BackCallback { back() }

    init {
        registerBackCallback(backCallback)
        coroutineScope.launch {
            state.value = State.ObtainCode
            log.debug { "start login" }
            val flow = makeLoginFlow()
            val newAuthRequestData =
                flow
                    .createAuthRequest()
                    .onFailure { exception ->
                        log.warn(exception) { "createAuthRequest failed" }
                        state.value = State.Failure(i18n.oAuth2LoginFailure(exception.message))
                        return@launch
                    }
                    .getOrThrow()
            log.debug { "createAuthRequest successful" }

            authRequestData.value = newAuthRequestData
            state.value =
                State.CheckCode(
                    userCode = newAuthRequestData.userCode,
                    uri = newAuthRequestData.verificationUri.toString(),
                    verificationUri = newAuthRequestData.verificationUriComplete?.toString(),
                )
            flow
                .waitForLogin()
                .onSuccess { authProviderData ->
                    log.debug { "login successful" }
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
                .onFailure { exception ->
                    log.warn(exception) { "could not check code" }
                    state.value = State.Failure(i18n.oAuth2LoginFailure(exception.message))
                }
        }
    }

    override fun openLoginPage() {
        authRequestData.value?.let { uriCaller((it.verificationUriComplete ?: it.verificationUri).toString(), true) }
    }

    override fun back() {
        onBack()
    }
}
