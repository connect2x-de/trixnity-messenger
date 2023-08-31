package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.GetAccountNames
import de.connect2x.trixnity.messenger.MatrixClientService
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.io.async.launch
import kotlinx.coroutines.flow.*
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface PasswordLoginViewModelFactory {
    fun newPasswordLoginViewModel(
        viewModelContext: ViewModelContext,
        serverUrl: String,
        matrixClientService: MatrixClientService,
        onLogin: () -> Unit,
        onBack: () -> Unit,
    ): PasswordLoginViewModel {
        return PasswordLoginViewModelImpl(
            viewModelContext,
            serverUrl,
            matrixClientService,
            onLogin,
            onBack,
        )
    }
}

interface PasswordLoginViewModel {
    val isFirstMatrixClient: StateFlow<Boolean?>
    val serverUrl: String

    val canLogin: StateFlow<Boolean>

    val accountName: MutableStateFlow<String>
    val username: MutableStateFlow<String>
    val password: MutableStateFlow<String>

    val addMatrixAccountState: StateFlow<AddMatrixAccountState>
    fun tryLogin()
    fun back()
}

open class PasswordLoginViewModelImpl(
    viewModelContext: ViewModelContext,
    override val serverUrl: String,
    private val matrixClientService: MatrixClientService,
    private val onLogin: () -> Unit,
    private val onBack: () -> Unit,
) : ViewModelContext by viewModelContext, PasswordLoginViewModel {

    private val accountNames = channelFlow { send(get<GetAccountNames>()()) }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    override val isFirstMatrixClient: StateFlow<Boolean?> = accountNames.map { it.isNullOrEmpty() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val accountName: MutableStateFlow<String> = MutableStateFlow(i18n.defaultAccountName())
    final override val username: MutableStateFlow<String> = MutableStateFlow("")
    final override val password: MutableStateFlow<String> = MutableStateFlow("")

    override val addMatrixAccountState: MutableStateFlow<AddMatrixAccountState> =
        MutableStateFlow(AddMatrixAccountState.None)

    override val canLogin: StateFlow<Boolean> =
        combine(
            accountName,
            username,
            password,
        ) { accountName, username, password ->
            log.trace { "canLogin: accountName=$accountName, username=$username, serverUrl=$serverUrl, password=${if (password.isNotBlank()) "***" else ""}" }
            val accountAlreadyExists = accountNames.value?.contains(accountName) ?: false
            if (accountAlreadyExists)
                addMatrixAccountState.value =
                    AddMatrixAccountState.Failure(i18n.accountAlreadyExistsLocally(accountName))
            accountAlreadyExists.not() && accountName.isNotBlank() && username.isNotBlank() && password.isNotBlank() && serverUrl.isNotBlank()
        }.stateIn(coroutineScope, SharingStarted.Eagerly, false) // eagerly because value is used below

    override fun tryLogin() {
        coroutineScope.launch {
            log.debug { "Try to login into $serverUrl with username=${username.value} and password=password=${if (password.value.isNotBlank()) "***" else ""}." }
            if (canLogin.value && addMatrixAccountState.value !is AddMatrixAccountState.Connecting) {
                matrixClientService.loginCatching(
                    accountName = accountName.value,
                    serverUrl = serverUrl,
                    username = username.value,
                    password = password.value,
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
    override val accountName: MutableStateFlow<String> = MutableStateFlow("default")
    override val username: MutableStateFlow<String> = MutableStateFlow("user")
    override val password: MutableStateFlow<String> = MutableStateFlow("password")
    override val addMatrixAccountState: StateFlow<AddMatrixAccountState> =
        MutableStateFlow(AddMatrixAccountState.Failure("dino"))

    override fun tryLogin() {
    }

    override fun back() {
    }

}