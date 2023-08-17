package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.GetAccountNames
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel.ServerDiscoveryState
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.serverDiscovery
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.clientserverapi.client.UIA
import org.koin.core.component.get
import kotlin.time.Duration.Companion.seconds


private val log = KotlinLogging.logger {}

interface AddMatrixAccountViewModelFactory {
    fun newAddMatrixAccountViewModel(
        viewModelContext: ViewModelContext,
        onSelectLoginType: (LoginType) -> Unit,
        onRegisterNewUser: () -> Unit,
        onCancel: () -> Unit,
    ): AddMatrixAccountViewModel {
        return AddMatrixAccountViewModelImpl(
            viewModelContext,
            onSelectLoginType,
            onRegisterNewUser,
            onCancel,
        )
    }
}

interface AddMatrixAccountViewModel {
    val isFirstMatrixClient: StateFlow<Boolean?>

    val serverUrl: MutableStateFlow<String>
    val serverDiscoveryState: StateFlow<ServerDiscoveryState>

    sealed interface ServerDiscoveryState {
        object None : ServerDiscoveryState
        object Loading : ServerDiscoveryState
        data class Success(val loginTypes: Set<LoginType>, val canRegisterNewUser: Boolean) : ServerDiscoveryState
        data class Failure(val message: String) : ServerDiscoveryState
    }

    fun selectLoginType(loginType: LoginType)
    fun registerNewUser() // FIXME use serverUrl

    fun cancel()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
open class AddMatrixAccountViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onSelectLoginType: (LoginType) -> Unit,
    private val onRegisterNewUser: () -> Unit,
    private val onCancel: () -> Unit,
) : ViewModelContext by viewModelContext, AddMatrixAccountViewModel {
    override val isFirstMatrixClient: StateFlow<Boolean?> =
        flow { emit(get<GetAccountNames>()()) }
            .map { it.isEmpty() }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    final override val serverUrl = MutableStateFlow("")

    final override val serverDiscoveryState =
        serverUrl.debounce(1.seconds).transformLatest { serverUrl ->
            when {
                serverUrl.isBlank() -> ServerDiscoveryState.None
                else -> {
                    emit(ServerDiscoveryState.Loading)
                    serverUrl.serverDiscovery()
                        .onFailure {
                            emit(ServerDiscoveryState.Failure(i18n.serverDiscoveryFailed()))
                        }
                        .onSuccess { serverDiscoveryUrl ->
                            val api = MatrixClientServerApiClientImpl(serverDiscoveryUrl)
                            val loginTypes = api.authentication.getLoginTypes().getOrThrow().flatMap { loginType ->
                                when (loginType) {
                                    is net.folivo.trixnity.clientserverapi.model.authentication.LoginType.Password ->
                                        setOf(LoginType.Password(serverDiscoveryUrl.toString()))

                                    is net.folivo.trixnity.clientserverapi.model.authentication.LoginType.SSO ->
                                        loginType.identityProviders.map {
                                            LoginType.SSO(
                                                serverUrl = serverDiscoveryUrl.toString(),
                                                id = it.id,
                                                name = it.name
                                            )
                                        }

                                    else -> setOf()
                                }
                            }.toSet()
                            val canRegisterNewUser = api.authentication.register().fold(
                                onSuccess = { it is UIA.Step<*> },
                                onFailure = { false }
                            )
                            emit(ServerDiscoveryState.Success(loginTypes, canRegisterNewUser))
                        }
                }
            }
        }.stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(),
            ServerDiscoveryState.None
        )

    override fun selectLoginType(loginType: LoginType) {
        onSelectLoginType(loginType)
    }

    override fun registerNewUser() {
        onRegisterNewUser()
    }

    override fun cancel() {
        onCancel()
    }
}

class PreviewAddMatrixAccountViewModel : AddMatrixAccountViewModel {
    override val isFirstMatrixClient: StateFlow<Boolean?> = MutableStateFlow(true)
    override val serverUrl: MutableStateFlow<String> = MutableStateFlow("")
    override val serverDiscoveryState: StateFlow<ServerDiscoveryState> = MutableStateFlow(ServerDiscoveryState.None)

    override fun selectLoginType(loginType: LoginType) {
    }

    override fun registerNewUser() {
    }

    override fun cancel() {
    }
}