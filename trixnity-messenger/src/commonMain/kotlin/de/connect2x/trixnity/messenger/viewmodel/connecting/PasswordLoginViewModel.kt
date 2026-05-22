package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.clientserverapi.client.MatrixClientAuthProviderData
import de.connect2x.trixnity.clientserverapi.client.classicLoginWithPassword
import de.connect2x.trixnity.clientserverapi.model.authentication.IdentifierType
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get
import org.koin.core.component.inject

interface PasswordLoginViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        serverUrl: String,
        onLogin: () -> Unit,
        onBack: () -> Unit,
    ): PasswordLoginViewModel {
        return PasswordLoginViewModelImpl(viewModelContext, serverUrl, onLogin, onBack)
    }

    companion object : PasswordLoginViewModelFactory
}

interface PasswordLoginViewModel {
    val isFirstMatrixClient: StateFlow<Boolean?>
    val serverUrl: String

    val canLogin: StateFlow<Boolean>

    val username: TextFieldViewModel
    val password: TextFieldViewModel

    val addMatrixAccountState: StateFlow<AddMatrixAccountState>

    fun tryLogin()

    fun back()
}

open class PasswordLoginViewModelImpl(
    viewModelContext: ViewModelContext,
    override val serverUrl: String,
    private val onLogin: () -> Unit,
    private val onBack: () -> Unit,
) : ViewModelContext by viewModelContext, PasswordLoginViewModel {

    private val config = get<MatrixMessengerConfiguration>()
    private val getDefaultDeviceDisplayName by inject<GetDefaultDeviceDisplayName>()
    override val isFirstMatrixClient: StateFlow<Boolean?> =
        matrixClients.map { it.isEmpty() }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    final override val username = TextFieldViewModelImpl(maxLength = 1_000)
    final override val password = TextFieldViewModelImpl(maxLength = 1_000)

    override val addMatrixAccountState: MutableStateFlow<AddMatrixAccountState> =
        MutableStateFlow(AddMatrixAccountState.None)

    override val canLogin: StateFlow<Boolean> =
        combine(username, password) { username, password ->
                log.trace {
                    "canLogin: username=$username, serverUrl=$serverUrl, password=${if (password.text.isNotBlank()) "***" else ""}"
                }
                username.text.isNotBlank() && password.text.isNotBlank() && serverUrl.isNotBlank()
            }
            .stateIn(coroutineScope, SharingStarted.Eagerly, false) // eagerly because value is used below

    override fun tryLogin() {
        coroutineScope.launch {
            log.debug {
                "Try to login into $serverUrl with username=${username.value} and password=password=${if (password.value.text.isNotBlank()) "***" else ""}."
            }
            if (canLogin.value && addMatrixAccountState.value !is AddMatrixAccountState.Connecting) {
                addMatrixAccountState.value = AddMatrixAccountState.Connecting
                MatrixClientAuthProviderData.classicLoginWithPassword(
                        baseUrl = Url(serverUrl),
                        identifier = IdentifierType.User(username.value.text),
                        password = password.value.text,
                        initialDeviceDisplayName = getDefaultDeviceDisplayName(),
                        refreshToken = config.useRefreshTokens,
                        httpClientEngine = config.httpClientEngine,
                        httpClientConfig = config.httpClientConfig,
                    )
                    .onClassicLoginFailure(i18n) { message ->
                        addMatrixAccountState.value = AddMatrixAccountState.Failure(message)
                    }
                    .onSuccess { authProviderData ->
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
                log.warn { "cannot login: canLogin=${canLogin.value}, serverUrl=${serverUrl}" }
            }
        }
    }

    override fun back() {
        onBack()
    }

    val backCallback = BackCallback { back() }

    init {
        registerBackCallback(backCallback)
    }
}

class PreviewPasswordLoginViewModel : PasswordLoginViewModel {
    override val serverUrl: String = "https://timmy-messenger.de"
    override val isFirstMatrixClient: StateFlow<Boolean?> = MutableStateFlow(false)
    override val canLogin: StateFlow<Boolean> = MutableStateFlow(false)
    override val username = TextFieldViewModelImpl(maxLength = 1_000)
    override val password = TextFieldViewModelImpl(maxLength = 1_000)
    override val addMatrixAccountState: StateFlow<AddMatrixAccountState> =
        MutableStateFlow(AddMatrixAccountState.Failure("dino"))

    override fun tryLogin() {}

    override fun back() {}
}
