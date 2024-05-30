package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.util.UriCaller
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.crypto.core.SecureRandom
import okio.ByteString.Companion.toByteString
import org.koin.core.component.get
import org.koin.core.component.inject


private val log = KotlinLogging.logger {}

interface SSOLoginViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        serverUrl: String,
        providerId: String,
        providerName: String,
        initialState: String? = null,
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
    val providerName: String

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
    fun resumeLogin(redirectUrl: Url)
    fun abortLogin()

    fun back()

    /**
     * Part of [resumeLogin] and thus should not be used directly. Should only be used to override the login process.
     */
    suspend fun loginWithLoginToken(loginToken: String)
}

open class SSOLoginViewModelImpl(
    viewModelContext: ViewModelContext,
    override val serverUrl: String,
    private val providerId: String,
    override val providerName: String,
    initialState: String? = null,
    private val onLogin: () -> Unit,
    private val onBack: () -> Unit,
) : ViewModelContext by viewModelContext, SSOLoginViewModel {
    private val getDefaultDeviceDisplayName by inject<GetDefaultDeviceDisplayName>()
    override val isFirstMatrixClient: StateFlow<Boolean?> = matrixClients.map { it.isEmpty() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val state: String = initialState
        ?: SecureRandom.nextBytes(16).toByteString().base64Url()
    private val uriCaller = get<UriCaller>()

    override val addMatrixAccountState: MutableStateFlow<AddMatrixAccountState> =
        MutableStateFlow(AddMatrixAccountState.None)
    private val messengerConfiguration = get<MatrixMessengerConfiguration>()

    private val redirectUrl =
        URLBuilder(messengerConfiguration.ssoRedirectPath).apply {
            protocol = URLProtocol.createOrDefault(messengerConfiguration.urlProtocol)
            host = messengerConfiguration.urlHost
            parameters.append("state", state)
        }.build()

    private val loginUrl =
        Url("$serverUrl/_matrix/client/v3/login/sso/redirect/$providerId?redirectUrl=$redirectUrl").toString()

    override val waitForRedirect: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isResumingLogin: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var loginJob: Job? = null

    override fun tryLogin() {
        coroutineScope.launch {
            waitForRedirect.value = true
            log.debug { "Persisting SSO state" }
            messengerSettings.update {
                it.copy(ssoState = SSOState(state, serverUrl, providerId, providerName))
            }
            log.debug { "Redirecting to $loginUrl" }
            try {
                uriCaller(loginUrl, false)
            } catch (exception: Exception) {
                log.warn(exception) { "could not open uri" }
            }
        }
    }

    override fun resumeLogin(redirectUrl: Url) {
        if (loginJob == null) {
            loginJob = coroutineScope.launch {
                waitForRedirect.value = false
                isResumingLogin.value = true
                val loginToken = if (redirectUrl.parameters["state"] == state) {
                    redirectUrl.parameters["loginToken"]
                } else null

                if (loginToken != null) {
                    log.debug { "Try to login into $serverUrl with loginToken=***." }
                    try {
                        loginWithLoginToken(loginToken)
                        addMatrixAccountState.value = AddMatrixAccountState.None
                    } finally {
                        log.debug { "Clearing stored sso login info" }
                        messengerSettings.update {
                            it.copy(ssoState = null)
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

    override suspend fun loginWithLoginToken(loginToken: String) {
        matrixClients.loginCatching(
            serverUrl = serverUrl,
            token = loginToken,
            initialDeviceDisplayName = getDefaultDeviceDisplayName(),
            addMatrixAccountState = addMatrixAccountState,
            i18n = i18n,
            onLogin = onLogin,
        )
    }

    override fun abortLogin() {
        loginJob?.cancel()
        coroutineScope.launch {
            log.debug { "Clearing stored sso login info" }
            waitForRedirect.value = false
            isResumingLogin.value = false
            messengerSettings.update { it.copy(ssoState = null) }
        }
    }

    override fun back() {
        abortLogin()
        onBack()
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

    override fun resumeLogin(redirectUrl: Url) {}
    override fun tryLogin() {}
    override fun abortLogin() {}
    override fun back() {}
    override suspend fun loginWithLoginToken(loginToken: String) {}
}
