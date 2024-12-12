package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel.ServerDiscoveryState
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import net.folivo.trixnity.client.serverDiscovery
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.authentication.LoginType
import net.folivo.trixnity.utils.takeBytes
import net.folivo.trixnity.utils.toByteArray
import net.folivo.trixnity.utils.toByteArrayFlow
import org.koin.core.component.get
import kotlin.time.Duration.Companion.seconds


private val log = KotlinLogging.logger {}

interface AddMatrixAccountViewModelFactory {
    fun create(
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

    companion object : AddMatrixAccountViewModelFactory
}

interface AddMatrixAccountViewModel {
    val isFirstMatrixClient: StateFlow<Boolean?>

    val serverUrl: MutableStateFlow<String>
    val serverDiscoveryState: StateFlow<ServerDiscoveryState>

    sealed interface ServerDiscoveryState {
        data object None : ServerDiscoveryState
        data object Loading : ServerDiscoveryState
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
) : ViewModelContext by viewModelContext, AddMatrixAccountViewModel {
    override val isFirstMatrixClient: StateFlow<Boolean?> =
        get<MatrixClients>()
            .map { it.isEmpty() }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    final override val serverUrl = MutableStateFlow(get<MatrixMessengerConfiguration>().defaultHomeServer ?: "")

    private val config = get<MatrixMessengerConfiguration>()

    final override val serverDiscoveryState =
        serverUrl.debounce(1.seconds).transformLatest { serverUrl ->
            when {
                serverUrl.isBlank() -> ServerDiscoveryState.None
                else -> {
                    emit(ServerDiscoveryState.Loading)
                    serverUrl.serverDiscovery(
                        httpClientEngine = config.httpClientEngine,
                        httpClientConfig = config.httpClientConfig
                    ).onFailure {
                        log.debug(it) { "server discovery failed" }
                        emit(ServerDiscoveryState.Failure(i18n.serverDiscoveryFailed()))
                    }.onSuccess { serverDiscoveryUrl ->
                        getLoginTypes(serverDiscoveryUrl)
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

    private suspend fun FlowCollector<ServerDiscoveryState>.getLoginTypes(
        serverDiscoveryUrl: Url
    ) {
        MatrixClientServerApiClientImpl(
            serverDiscoveryUrl,
            httpClientEngine = config.httpClientEngine,
            httpClientConfig = config.httpClientConfig
        ).use { api ->
            api.authentication.getLoginTypes()
                .onFailure {
                    log.debug(it) { "get login types failed" }
                    emit(ServerDiscoveryState.Failure(i18n.serverDiscoveryFailed()))
                }.onSuccess { loginTypes ->
                    log.debug { "get login types succeeded" }
                    val addMatrixAccountMethods = loginTypes.flatMap { loginType ->
                        when (loginType) {
                            is LoginType.Password ->
                                setOf(AddMatrixAccountMethod.Password(serverDiscoveryUrl.toString()))

                            is LoginType.SSO ->
                                loginType.identityProviders.map { identityProvider ->
                                    val icon = identityProvider.icon?.let {
                                        var byteArray: ByteArray? = null
                                        api.media.downloadLegacy(it) {
                                            byteArray =
                                                it.content.toByteArrayFlow()
                                                    .takeBytes(10 * 1024 * 1024) // max 10 MB
                                                    .toByteArray()
                                        }.onFailure {
                                            log.warn { "could not download idp icon $it" }
                                        }.getOrNull()
                                        byteArray
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

class PreviewAddMatrixAccountViewModel : AddMatrixAccountViewModel {
    override val isFirstMatrixClient: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val serverUrl: MutableStateFlow<String> = MutableStateFlow("")
    override val serverDiscoveryState: MutableStateFlow<ServerDiscoveryState> =
        MutableStateFlow(ServerDiscoveryState.None)

    override fun selectAddMatrixAccountMethod(addMatrixAccountMethod: AddMatrixAccountMethod) {
    }

    override fun cancel() {
    }
}
