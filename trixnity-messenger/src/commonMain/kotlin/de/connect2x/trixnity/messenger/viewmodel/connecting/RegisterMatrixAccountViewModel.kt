package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountState.None
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaResult
import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import de.connect2x.trixnity.clientserverapi.client.ClassicMatrixClientAuthProviderData
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import de.connect2x.trixnity.clientserverapi.model.authentication.AccountType
import de.connect2x.trixnity.core.ErrorResponse
import org.koin.core.component.get

interface RegisterMatrixAccountViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        serverUrl: String,
        onLogin: () -> Unit,
        onBack: () -> Unit,
    ): RegisterMatrixAccountViewModel =
        RegisterMatrixAccountViewModelImpl(viewModelContext, serverUrl, onLogin, onBack)

    companion object : RegisterMatrixAccountViewModelFactory
}

interface RegisterMatrixAccountViewModel {
    val isFirstMatrixClient: StateFlow<Boolean?>

    val error: StateFlow<String?>
    val serverUrl: String

    val username: TextFieldViewModel
    val password: TextFieldViewModel

    val isRegisteringNewUser: StateFlow<Boolean>
    val canRegisterNewUser: StateFlow<Boolean>
    val addMatrixAccountState: StateFlow<AddMatrixAccountState>

    fun register()
    fun back()
}

class RegisterMatrixAccountViewModelImpl(
    viewModelContext: ViewModelContext,
    override val serverUrl: String,
    private val onLogin: () -> Unit,
    private val onBack: () -> Unit,
) : RegisterMatrixAccountViewModel, ViewModelContext by viewModelContext {

    private val authorizeUia = get<AuthorizeUia>()
    private val config = get<MatrixMessengerConfiguration>()
    private val getDefaultDeviceDisplayName = get<GetDefaultDeviceDisplayName>()
    override val isFirstMatrixClient: StateFlow<Boolean?> = matrixClients.map { it.isEmpty() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    override val username = TextFieldViewModelImpl(maxLength = 1_000)
    override val password = TextFieldViewModelImpl(maxLength = 1_000)

    override val addMatrixAccountState: MutableStateFlow<AddMatrixAccountState> = MutableStateFlow(None)

    override val isRegisteringNewUser: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canRegisterNewUser: StateFlow<Boolean> = combine(
        username.text, password.text
    ) { username, password ->
        log.debug { "canRegisterNewUser: username=$username" }
        username.isNotBlank() && password.isNotBlank()
    }.stateIn(coroutineScope, SharingStarted.Eagerly, false) // is used down below

    override fun register() {
        val canRegisterNewUser = canRegisterNewUser.value
        log.info { "try registration (canRegisterNewUser = $canRegisterNewUser, isRegisteringNewUser=${isRegisteringNewUser.value} username = ${username.value}, password = *******)" }
        if (canRegisterNewUser && isRegisteringNewUser.getAndUpdate { true }.not()) {
            coroutineScope.launch {
                error.value = null
                val result = MatrixClientServerApiClientImpl(
                    Url(serverUrl),
                    httpClientEngine = config.httpClientEngine,
                    httpClientConfig = config.httpClientConfig
                ).use {
                    authorizeUia {
                        it.authentication.register(
                            accountType = AccountType.USER,
                            username = username.textValue,
                            password = password.textValue,
                            initialDeviceDisplayName = getDefaultDeviceDisplayName(),
                            refreshToken = config.useRefreshTokens,
                        )
                    }
                }
                when (result) {
                    is AuthorizeUiaResult.Success -> {
                        log.info { "try to do UIA to retrieve access_token" }
                        val deviceId = result.uia.value.deviceId
                        val accessToken = result.uia.value.accessToken
                        val accessTokenExpiresInMs = result.uia.value.accessTokenExpiresInMs
                        val refreshToken = result.uia.value.refreshToken
                        if (deviceId != null && accessToken != null) {
                            addMatrixAccountState.value = AddMatrixAccountState.Connecting

                            val authProviderData = ClassicMatrixClientAuthProviderData(
                                baseUrl = Url(serverUrl),
                                accessToken = accessToken,
                                accessTokenExpiresInMs = accessTokenExpiresInMs,
                                refreshToken = refreshToken,
                            )
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
                        } else {
                            log.error { "accessToken or deviceId missing in registration response" }
                            error.value = i18n.registrationErrorNotSuccessful()
                        }
                    }

                    is AuthorizeUiaResult.CancelledByUser -> {
                        error.value = result.message
                    }

                    is AuthorizeUiaResult.Error -> {
                        error.value = when (result.exception.errorResponse) {
                            is ErrorResponse.UserInUse -> i18n.registrationErrorUserInUse()
                            is ErrorResponse.InvalidUsername -> i18n.registrationErrorInvalidUsername()
                            is ErrorResponse.Exclusive -> i18n.registrationErrorInvalidUsername() // for users this is the same
                            else -> i18n.registrationErrorNotSupported()
                        }
                    }

                    is AuthorizeUiaResult.UnexpectedError -> {
                        error.value = result.message
                    }
                }

            }.invokeOnCompletion { isRegisteringNewUser.value = false }
        }
    }

    override fun back() {
        onBack()
    }

    val backCallback = BackCallback {
        back()
    }

    init {
        registerBackCallback(backCallback)
    }
}

class PreviewRegisterMatrixAccountViewModel : RegisterMatrixAccountViewModel {
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isFirstMatrixClient: StateFlow<Boolean?> = MutableStateFlow(false)
    override val serverUrl: String = "http://localhost:8008"
    override val username = TextFieldViewModelImpl(maxLength = 1_000, "user1")
    override val password = TextFieldViewModelImpl(maxLength = 1_000, "user1-password")
    override val addMatrixAccountState: MutableStateFlow<AddMatrixAccountState> = MutableStateFlow(None)
    override val isRegisteringNewUser: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canRegisterNewUser: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override fun register() {}
    override fun back() {}
}
