package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountState.None
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaResult
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.clientserverapi.model.authentication.AccountType
import net.folivo.trixnity.core.ErrorResponse
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
    val serverUrl: String

    val username: MutableStateFlow<String>
    val password: MutableStateFlow<String>

    val isRegisteringNewUser: StateFlow<Boolean>
    val canRegisterNewUser: StateFlow<Boolean>
    val addMatrixAccountState: StateFlow<AddMatrixAccountState>

    fun register()
    fun back()
}

class RegisterNewAccountViewModelImpl(
    viewModelContext: ViewModelContext,
    override val serverUrl: String,
    private val onLogin: () -> Unit,
    private val onBack: () -> Unit,
) : RegisterNewAccountViewModel, ViewModelContext by viewModelContext {

    private val authorizeUia = get<AuthorizeUia>()
    private val config = get<MatrixMessengerConfiguration>()
    private val getDefaultDeviceDisplayName = get<GetDefaultDeviceDisplayName>()
    override val isFirstMatrixClient: StateFlow<Boolean?> = matrixClients.map { it.isEmpty() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    override val username: MutableStateFlow<String> = MutableStateFlow("")
    override val password: MutableStateFlow<String> = MutableStateFlow("")

    override val addMatrixAccountState: MutableStateFlow<AddMatrixAccountState> = MutableStateFlow(None)

    override val isRegisteringNewUser: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canRegisterNewUser: StateFlow<Boolean> = combine(
        username, password
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
                            username = username.value,
                            password = password.value,
                            initialDeviceDisplayName = getDefaultDeviceDisplayName()
                        )
                    }
                }
                when (result) {
                    is AuthorizeUiaResult.Success -> {
                        log.info { "try to do UIA to retrieve access_token" }
                        val deviceId = result.uia.value.deviceId
                        val accessToken = result.uia.value.accessToken
                        if (deviceId != null && accessToken != null) {
                            matrixClients.loginWithCatching(
                                baseUrl = serverUrl,
                                loginInfo = MatrixClient.LoginInfo(
                                    userId = result.uia.value.userId,
                                    deviceId = deviceId,
                                    accessToken = accessToken,
                                ),
                                addMatrixAccountState = addMatrixAccountState,
                                i18n = i18n,
                                onLogin = onLogin,
                            )
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
}

class PreviewRegisterNewAccountViewModel : RegisterNewAccountViewModel {
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isFirstMatrixClient: StateFlow<Boolean?> = MutableStateFlow(false)
    override val serverUrl: String = "http://localhost:8008"
    override val username: MutableStateFlow<String> = MutableStateFlow("user1")
    override val password: MutableStateFlow<String> = MutableStateFlow("user1-password")
    override val addMatrixAccountState: MutableStateFlow<AddMatrixAccountState> = MutableStateFlow(None)
    override val isRegisteringNewUser: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canRegisterNewUser: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override fun register() {}
    override fun back() {}
}
