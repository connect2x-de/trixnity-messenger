package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import org.koin.core.component.inject


private val log = KotlinLogging.logger {}

interface PasswordLoginViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        serverUrl: String,
        onLogin: (MatrixClient) -> Unit,
        onBack: () -> Unit,
    ): PasswordLoginViewModel {
        return PasswordLoginViewModelImpl(
            viewModelContext,
            serverUrl,
            onLogin,
            onBack,
        )
    }

    companion object : PasswordLoginViewModelFactory
}

interface PasswordLoginViewModel {
    val isFirstMatrixClient: StateFlow<Boolean?>
    val serverUrl: String

    val canLogin: StateFlow<Boolean>

    val username: MutableStateFlow<String>
    val password: MutableStateFlow<String>

    val addMatrixAccountState: StateFlow<AddMatrixAccountState>
    fun tryLogin()
    fun back()
}

open class PasswordLoginViewModelImpl(
    viewModelContext: ViewModelContext,
    override val serverUrl: String,
    private val onLogin: (MatrixClient) -> Unit,
    private val onBack: () -> Unit,
) : ViewModelContext by viewModelContext, PasswordLoginViewModel {

    private val getDefaultDeviceDisplayName by inject<GetDefaultDeviceDisplayName>()
    override val isFirstMatrixClient: StateFlow<Boolean?> = matrixClients.map { it.isEmpty() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    final override val username: MutableStateFlow<String> = MutableStateFlow("")
    final override val password: MutableStateFlow<String> = MutableStateFlow("")

    override val addMatrixAccountState: MutableStateFlow<AddMatrixAccountState> =
        MutableStateFlow(AddMatrixAccountState.None)

    override val canLogin: StateFlow<Boolean> =
        combine(
            username,
            password,
        ) { username, password ->
            log.trace { "canLogin: username=$username, serverUrl=$serverUrl, password=${if (password.isNotBlank()) "***" else ""}" }
            username.isNotBlank() && password.isNotBlank() && serverUrl.isNotBlank()
        }.stateIn(coroutineScope, SharingStarted.Eagerly, false) // eagerly because value is used below

    override fun tryLogin() {
        coroutineScope.launch {
            log.debug { "Try to login into $serverUrl with username=${username.value} and password=password=${if (password.value.isNotBlank()) "***" else ""}." }
            if (canLogin.value && addMatrixAccountState.value !is AddMatrixAccountState.Connecting) {
                matrixClients.loginCatching(
                    serverUrl = serverUrl,
                    username = username.value,
                    password = password.value,
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

class PreviewPasswordLoginViewModel : PasswordLoginViewModel {
    override val serverUrl: String = "https://timmy-messenger.de"
    override val isFirstMatrixClient: StateFlow<Boolean?> = MutableStateFlow(false)
    override val canLogin: StateFlow<Boolean> = MutableStateFlow(false)
    override val username: MutableStateFlow<String> = MutableStateFlow("user")
    override val password: MutableStateFlow<String> = MutableStateFlow("password")
    override val addMatrixAccountState: StateFlow<AddMatrixAccountState> =
        MutableStateFlow(AddMatrixAccountState.Failure("dino"))

    override fun tryLogin() {
    }

    override fun back() {
    }

}
