package de.connect2x.trixnity.messenger.viewmodel.connecting

import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.util.UrlHandler
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    val canLogin: StateFlow<Boolean>
    val addMatrixAccountState: StateFlow<AddMatrixAccountState>

    val loginToken: MutableStateFlow<String>

    val loginUrl: String
    fun tryLogin()

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

    override val loginToken: MutableStateFlow<String> = MutableStateFlow("")

    override val addMatrixAccountState: MutableStateFlow<AddMatrixAccountState> =
        MutableStateFlow(AddMatrixAccountState.None)
    private val messengerConfiguration = get<MatrixMessengerConfiguration>()

    private val redirectUrl =
        URLBuilder(messengerConfiguration.ssoRedirectPath).apply {
            protocol = URLProtocol.createOrDefault(messengerConfiguration.urlProtocol)
            host = messengerConfiguration.urlHost
            parameters.append("id", uuid4().toString()) // TODO need to be cached in the web!
        }.build()

    init {
        val urlHandler = get<UrlHandler>()
        coroutineScope.launch {
            urlHandler.filter {
                it.encodedPath == redirectUrl.encodedPath
                        && it.parameters["id"] == redirectUrl.parameters["id"]
            }.collect {
                val loginToken = it.parameters["loginToken"]
                if (loginToken != null)
                    this@SSOLoginViewModelImpl.loginToken.value = loginToken
            }
        }
    }

    override val loginUrl =
        Url("$serverUrl/_matrix/client/v3/login/sso/redirect/$providerId?redirectUrl=$redirectUrl").toString()

    override val canLogin: StateFlow<Boolean> =
        loginToken.map { loginToken ->
            log.trace { "canLogin: loginToken=${if (loginToken.isNotBlank()) "***" else ""}, serverUrl=$serverUrl" }
            loginToken.isNotBlank() && serverUrl.isNotBlank()
        }
            .stateIn(coroutineScope, SharingStarted.Eagerly, false) // eagerly because value is used below

    override fun tryLogin() {
        coroutineScope.launch {
            log.debug { "Try to login into $serverUrl with loginToken=${if (loginToken.value.isNotBlank()) "***" else ""}." }
            if (canLogin.value && addMatrixAccountState.value !is AddMatrixAccountState.Connecting) {
                matrixClients.loginCatching(
                    serverUrl = serverUrl,
                    token = loginToken.value,
                    initialDeviceDisplayName = getDefaultDeviceDisplayName(),
                    addMatrixAccountState = addMatrixAccountState,
                    i18n = i18n,
                    onLogin = onLogin,
                )
            } else {
                log.warn { "cannot login: canLogin=${canLogin.value}, serverUrl=${serverUrl}" }
            }
        }
    }

    override fun back() {
        onBack()
    }
}

class PreviewSSOLoginViewModel : SSOLoginViewModel {
    override val serverUrl: String = "https://timmy-messenger.de"
    override val isFirstMatrixClient: StateFlow<Boolean?> = MutableStateFlow(false)
    override val providerName: String = "Timmy"
    override val canLogin: StateFlow<Boolean> = MutableStateFlow(false)
    override val loginToken: MutableStateFlow<String> = MutableStateFlow("")
    override val addMatrixAccountState: StateFlow<AddMatrixAccountState> =
        MutableStateFlow(AddMatrixAccountState.Failure("dino"))

    override val loginUrl: String =
        Url("$serverUrl/_matrix/client/v3/login/sso/redirect?redirectUrl=trixnity://sso").toString()

    override fun tryLogin() {
    }

    override fun back() {
    }

}