package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.util.UriCaller
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountState.None
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.path
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import de.connect2x.trixnity.clientserverapi.client.MatrixClientAuthProviderData
import de.connect2x.trixnity.clientserverapi.client.classicLoginWithToken
import de.connect2x.trixnity.crypto.core.SecureRandom
import de.connect2x.trixnity.utils.nextString
import org.koin.core.component.get
import org.koin.core.component.inject

interface SSOLoginViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        serverUrl: String,
        providerId: String?,
        providerName: String?,
        initialState: String?,
        onLogin: () -> Unit,
        onBack: () -> Unit,
    ): SSOLoginViewModel {
        return SSOLoginViewModelImpl(
            viewModelContext,
            serverUrl,
            providerId,
            providerName,
            initialState,
            onLogin,
            onBack,
        )
    }

    companion object : SSOLoginViewModelFactory
}

interface SSOLoginViewModel {
    val isFirstMatrixClient: StateFlow<Boolean?>
    val serverUrl: String
    val providerName: String?

    val addMatrixAccountState: StateFlow<AddMatrixAccountState>

    /**
     * Is true, when the viewmodel is waiting for the SSO provider login page to redirect back to the viewmodel.
     */
    val waitForRedirect: StateFlow<Boolean>

    /**
     * Is true, when the redirect from the SSO provider login page happened.
     */
    val isResumingLogin: StateFlow<Boolean>

    /**
     * Opens SSO provider login page, waits to receive the token and logs in with this token.
     */
    fun tryLogin()

    /**
     * Resume a previously started SSO flow with a given redirect URL
     */
    fun resumeLogin(redirectUri: String)
    fun abortLogin()

    fun back()
}

open class SSOLoginViewModelImpl(
    viewModelContext: ViewModelContext,
    override val serverUrl: String,
    private val providerId: String?,
    override val providerName: String?,
    initialState: String? = null,
    private val onLogin: () -> Unit,
    private val onBack: () -> Unit,
) : ViewModelContext by viewModelContext, SSOLoginViewModel {
    private val getDefaultDeviceDisplayName by inject<GetDefaultDeviceDisplayName>()
    override val isFirstMatrixClient: StateFlow<Boolean?> = matrixClients.map { it.isEmpty() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val state: String = initialState ?: SecureRandom.nextString(32)
    private val uriCaller = get<UriCaller>()

    override val addMatrixAccountState: MutableStateFlow<AddMatrixAccountState> = MutableStateFlow(None)
    private val config = get<MatrixMessengerConfiguration>()

    private val redirectUrl =
        URLBuilder("${config.appUri}/${config.appUriSsoRedirect}").apply {
            parameters.append("state", state)
        }.build()

    private val loginUrl =
        URLBuilder(serverUrl).apply {
            if (providerId != null) {
                path("/_matrix/client/v3/login/sso/redirect/$providerId")
            } else {
                path("/_matrix/client/v3/login/sso/redirect")
            }
            parameters.append("redirectUrl", redirectUrl.toString())
        }.build().toString()

    override val waitForRedirect: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isResumingLogin: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var loginJob: Job? = null

    override fun tryLogin() {
        coroutineScope.launch {
            waitForRedirect.value = true
            log.debug { "Persisting SSO state" }
            messengerSettings.update<MatrixMessengerSettingsBase> {
                it.copy(
                    ssoLoginState = MatrixMessengerSettingsBase.SSOLoginState(
                        state = state,
                        serverUrl = serverUrl,
                        providerId = providerId,
                        providerName = providerName
                    )
                )
            }
            log.debug { "Redirecting to $loginUrl" }
            try {
                uriCaller(loginUrl, false)
            } catch (exception: Exception) {
                log.warn(exception) { "could not open uri" }
            }
        }
    }

    override fun resumeLogin(redirectUri: String) {
        if (loginJob == null) {
            loginJob = coroutineScope.launch {
                log.debug { "begin resume login job" }
                waitForRedirect.value = false
                isResumingLogin.value = true
                val redirectUri = Url(redirectUri)
                val loginToken = if (redirectUri.parameters["state"] == state) {
                    redirectUri.parameters["loginToken"]
                } else null

                if (loginToken != null) {
                    log.debug { "Try to login into $serverUrl with loginToken=***." }
                    addMatrixAccountState.value = AddMatrixAccountState.Connecting
                    messengerSettings.update<MatrixMessengerSettingsBase> {
                        it.copy(ssoLoginState = null)
                    }
                    MatrixClientAuthProviderData.classicLoginWithToken(
                        baseUrl = Url(serverUrl),
                        token = loginToken,
                        initialDeviceDisplayName = getDefaultDeviceDisplayName(),
                        refreshToken = config.useRefreshTokens,
                        httpClientEngine = config.httpClientEngine,
                        httpClientConfig = config.httpClientConfig,
                    ).onClassicLoginFailure(i18n) { message ->
                        addMatrixAccountState.value = AddMatrixAccountState.Failure(message)
                    }.onSuccess { authProviderData ->
                        when (val createMatrixClientResult = matrixClients.create(authProviderData)) {
                            is MatrixClients.CreateResult.Success -> {
                                addMatrixAccountState.value = AddMatrixAccountState.Success
                                onLogin()
                            }

                            is MatrixClients.CreateResult.Failure -> {
                                addMatrixAccountState.value =
                                    AddMatrixAccountState.Failure(createMatrixClientResult.message)
                            }
                        }
                    }
                } else {
                    log.warn { "Could not resume login: no token matching the correct state was found" }
                }
            }
            loginJob?.invokeOnCompletion {
                loginJob = null
                isResumingLogin.value = false
            }
        }
    }

    override fun abortLogin() {
        loginJob?.cancel("abort login")
        coroutineScope.launch {
            log.debug { "Clearing stored sso login info" }
            waitForRedirect.value = false
            isResumingLogin.value = false
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

    init {
        registerBackCallback(backCallback)
    }
}

class PreviewSSOLoginViewModel : SSOLoginViewModel {
    override val serverUrl: String = "https://timmy-messenger.de"
    override val isFirstMatrixClient: StateFlow<Boolean?> = MutableStateFlow(false)
    override val providerName: String = "Timmy"
    override val addMatrixAccountState: StateFlow<AddMatrixAccountState> =
        MutableStateFlow(AddMatrixAccountState.Failure("dino"))
    override val waitForRedirect: StateFlow<Boolean> = MutableStateFlow(true)
    override val isResumingLogin: StateFlow<Boolean> = MutableStateFlow(false)

    override fun resumeLogin(redirectUri: String) {}
    override fun tryLogin() {}
    override fun abortLogin() {}
    override fun back() {}
}
