package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.util.UriCaller
import de.connect2x.trixnity.messenger.util.UrlHandler
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import korlibs.io.async.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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
        onLogin: () -> Unit,
        onBack: () -> Unit,
    ): SSOLoginViewModel {
        return SSOLoginViewModelImpl(
            viewModelContext,
            serverUrl,
            providerId,
            providerName,
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
    val waitForRedirect: StateFlow<Boolean>

    fun tryLogin()
    fun abortLogin()

    fun back()
}

open class SSOLoginViewModelImpl(
    viewModelContext: ViewModelContext,
    override val serverUrl: String,
    providerId: String,
    override val providerName: String,
    private val onLogin: () -> Unit,
    private val onBack: () -> Unit,
) : ViewModelContext by viewModelContext, SSOLoginViewModel {
    private val getDefaultDeviceDisplayName by inject<GetDefaultDeviceDisplayName>()
    override val isFirstMatrixClient: StateFlow<Boolean?> = matrixClients.map { it.isEmpty() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val state = SecureRandom.nextBytes(16).toByteString().base64Url()// TODO need to be cached in the web!
    private val urlHandler = get<UrlHandler>()
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

    private var loginJob: Job? = null
    override fun tryLogin() {
        if (loginJob != null) {
            loginJob = coroutineScope.launch {
                waitForRedirect.value = true
                val loginToken = async {
                    urlHandler.filter {
                        it.encodedPath == redirectUrl.encodedPath
                                && it.parameters["state"] == state
                    }.map {
                        it.parameters["loginToken"]
                    }.filterNotNull().first()
                }
                uriCaller(loginUrl)
                loginToken.await()
                waitForRedirect.value = false
                log.debug { "Try to login into $serverUrl with loginToken=***." }
                matrixClients.loginCatching(
                    serverUrl = serverUrl,
                    token = loginToken.await(),
                    initialDeviceDisplayName = getDefaultDeviceDisplayName(),
                    addMatrixAccountState = addMatrixAccountState,
                    i18n = i18n,
                    onLogin = onLogin,
                )
            }
            loginJob?.invokeOnCompletion {
                addMatrixAccountState.value = AddMatrixAccountState.None
                waitForRedirect.value = false
                loginJob = null
            }
        }
    }

    override fun abortLogin() {
        loginJob?.cancel()
    }

    override fun back() {
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


    override fun tryLogin() {
    }

    override fun abortLogin() {
    }

    override fun back() {
    }

}
