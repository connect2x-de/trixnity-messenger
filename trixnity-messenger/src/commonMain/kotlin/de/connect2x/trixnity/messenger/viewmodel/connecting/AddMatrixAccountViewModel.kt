package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.GetAccountNames
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel.ServerDiscoveryState
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.util.*
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
        onAddMatrixAccountMethod: (AddMatrixAccountMethod) -> Unit,
        onCancel: () -> Unit,
    ): AddMatrixAccountViewModel {
        return AddMatrixAccountViewModelImpl(
            viewModelContext,
            onAddMatrixAccountMethod,
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
        data class Success(val addMatrixAccountMethods: Set<AddMatrixAccountMethod>) : ServerDiscoveryState
        data class Failure(val message: String) : ServerDiscoveryState
    }

    fun selectAddMatrixAccountMethod(addMatrixAccountMethod: AddMatrixAccountMethod)
    fun cancel()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
open class AddMatrixAccountViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onAddMatrixAccountMethod: (AddMatrixAccountMethod) -> Unit,
    private val onCancel: () -> Unit,
    private val httpClientFactory: (HttpClientConfig<*>.() -> Unit) -> HttpClient = { HttpClient(it) },
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
                    serverUrl.serverDiscovery(httpClientFactory = httpClientFactory)
                        .onFailure {
                            log.debug(it) { "server discovery failed" }
                            emit(ServerDiscoveryState.Failure(i18n.serverDiscoveryFailed()))
                        }
                        .onSuccess { serverDiscoveryUrl ->
                            val api = MatrixClientServerApiClientImpl(
                                serverDiscoveryUrl,
                                httpClientFactory = httpClientFactory
                            )
                            api.authentication.getLoginTypes()
                                .onFailure {
                                    log.debug(it) { "get login types failed" }
                                    emit(ServerDiscoveryState.Failure(i18n.serverDiscoveryFailed()))
                                }
                                .onSuccess { loginTypes ->
                                    val addMatrixAccountMethods = loginTypes.flatMap { loginType ->
                                        when (loginType) {
                                            is net.folivo.trixnity.clientserverapi.model.authentication.LoginType.Password ->
                                                setOf(AddMatrixAccountMethod.Password(serverDiscoveryUrl.toString()))

                                            is net.folivo.trixnity.clientserverapi.model.authentication.LoginType.SSO ->
                                                loginType.identityProviders.map { identityProvider ->
                                                    val icon = identityProvider.icon?.let {
                                                        api.media.download(it).onFailure {
                                                            log.warn { "could not download idp icon $it" }
                                                        }.getOrNull()?.content?.toByteArray()
                                                    }
                                                    AddMatrixAccountMethod.SSO(
                                                        serverUrl = serverDiscoveryUrl.toString(),
                                                        identityProvider = identityProvider,
                                                        icon = icon,
                                                    )
                                                }

                                            else -> setOf()
                                        }
                                    }.toSet()
                                    val canRegisterNewUser = api.authentication.register()
                                        .fold(
                                            onSuccess = { it is UIA.Step<*> },
                                            onFailure = { false }
                                        )
                                    emit(
                                        ServerDiscoveryState.Success(
                                            addMatrixAccountMethods =
                                            if (canRegisterNewUser)
                                                addMatrixAccountMethods + AddMatrixAccountMethod.Register(
                                                    serverDiscoveryUrl.toString()
                                                )
                                            else addMatrixAccountMethods
                                        )
                                    )
                                }
                        }
                }
            }
        }.stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(),
            ServerDiscoveryState.None
        )

    override fun selectAddMatrixAccountMethod(addMatrixAccountMethod: AddMatrixAccountMethod) {
        onAddMatrixAccountMethod(addMatrixAccountMethod)
    }

    override fun cancel() {
        onCancel()
    }
}

class PreviewAddMatrixAccountViewModel : AddMatrixAccountViewModel {
    override val isFirstMatrixClient: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val serverUrl: MutableStateFlow<String> = MutableStateFlow("")
    override val serverDiscoveryState: MutableStateFlow<ServerDiscoveryState> = MutableStateFlow(ServerDiscoveryState.None)

    override fun selectAddMatrixAccountMethod(addMatrixAccountMethod: AddMatrixAccountMethod) {
    }

    override fun cancel() {
    }
}