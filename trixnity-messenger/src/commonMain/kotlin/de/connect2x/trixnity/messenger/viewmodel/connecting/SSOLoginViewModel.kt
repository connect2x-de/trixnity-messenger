package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.GetAccountNames
import de.connect2x.trixnity.messenger.MatrixClientService
import de.connect2x.trixnity.messenger.util.UrlHandler
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import korlibs.io.async.launch
import kotlinx.coroutines.flow.*
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface SSOLoginViewModelFactory {
    fun newSSOLoginViewModel(
        viewModelContext: ViewModelContext,
        matrixClientService: MatrixClientService,
        serverUrl: String,
        providerId: String,
        providerName: String,
        onLogin: () -> Unit,
        onBack: () -> Unit,
    ): SSOLoginViewModel {
        return SSOLoginViewModelImpl(
            viewModelContext,
            matrixClientService,
            serverUrl,
            providerId,
            providerName,
            onLogin,
            onBack,
        )
    }
}

interface SSOLoginViewModel {
    val isFirstMatrixClient: StateFlow<Boolean?>
    val serverUrl: String
    val providerName: String

    val canLogin: StateFlow<Boolean>
    val addMatrixAccountState: StateFlow<AddMatrixAccountState>

    val accountName: MutableStateFlow<String>
    val loginToken: MutableStateFlow<String>

    val loginUrl: String
    fun tryLogin()

    fun back()
}

open class SSOLoginViewModelImpl(
    viewModelContext: ViewModelContext,
    private val matrixClientService: MatrixClientService,
    override val serverUrl: String,
    providerId: String,
    override val providerName: String,
    private val onLogin: () -> Unit,
    private val onBack: () -> Unit,
) : ViewModelContext by viewModelContext, SSOLoginViewModel {
    private val accountNames = channelFlow { send(get<GetAccountNames>()()) }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    override val isFirstMatrixClient: StateFlow<Boolean?> = accountNames.map { it.isNullOrEmpty() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val accountName: MutableStateFlow<String> = MutableStateFlow(i18n.defaultAccountName())
    override val loginToken: MutableStateFlow<String> = MutableStateFlow("")

    override val addMatrixAccountState: MutableStateFlow<AddMatrixAccountState> =
        MutableStateFlow(AddMatrixAccountState.None)
    private val urlHandler = get<UrlHandler>()
    private val messengerSettings = get<MessengerSettings>()

    private val redirectUrl =
        URLBuilder(messengerSettings.ssoRedirectPath).apply {
            protocol = URLProtocol.createOrDefault(messengerSettings.urlProtocol)
            host = messengerSettings.urlHost
        }.build()

    init {
        coroutineScope.launch {
            urlHandler.filter {
                it.encodedPath == redirectUrl.encodedPath
            }.collect { // FIXME unique id to prevent cross site attacks
                val loginToken = it.parameters["loginToken"]
                if (loginToken != null)
                    this.loginToken.value = loginToken
            }
        }
    }

    override val loginUrl =
        Url("$serverUrl/_matrix/client/v3/login/sso/redirect/$providerId?redirectUrl=$redirectUrl").toString()

    override val canLogin: StateFlow<Boolean> =
        combine(
            accountName,
            loginToken,
        ) { accountName, loginToken ->
            log.trace { "canLogin: accountName=$accountName, loginToken=$loginToken, serverUrl=$serverUrl" }
            val accountAlreadyExists = accountNames.value?.contains(accountName) ?: false
            if (accountAlreadyExists)
                addMatrixAccountState.value =
                    AddMatrixAccountState.Failure(i18n.accountAlreadyExistsLocally(accountName))
            accountAlreadyExists.not() && accountName.isNotBlank() && loginToken.isNotBlank() && serverUrl.isNotBlank()
        }.stateIn(coroutineScope, SharingStarted.Eagerly, false) // eagerly because value is used below

    override fun tryLogin() {
        coroutineScope.launch {
            log.debug { "Try to login into $serverUrl with loginToken ${loginToken.value} and password *************." }
            if (canLogin.value && addMatrixAccountState.value !is AddMatrixAccountState.Connecting) {
                matrixClientService.loginCatching(
                    accountName = accountName.value,
                    serverUrl = serverUrl,
                    token = loginToken.value,
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
    override val accountName: MutableStateFlow<String> = MutableStateFlow("default")
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