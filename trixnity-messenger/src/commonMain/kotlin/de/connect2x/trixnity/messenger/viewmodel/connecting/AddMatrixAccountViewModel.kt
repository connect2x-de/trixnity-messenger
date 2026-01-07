package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel.ServerDiscoveryState
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.serverDiscovery
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.authentication.LoginType
import net.folivo.trixnity.clientserverapi.model.authentication.oauth2.PromptValue
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

    val serverUrl: TextFieldViewModel
    val serverDiscoveryState: StateFlow<ServerDiscoveryState>

    val isMultiProfile: StateFlow<Boolean>

    sealed interface ServerDiscoveryState {
        data object None : ServerDiscoveryState
        data object Loading : ServerDiscoveryState
        data class Success(val addMatrixAccountMethods: Set<AddMatrixAccountMethod>) : ServerDiscoveryState
        data class Failure(val message: String) : ServerDiscoveryState
    }

    fun selectAddMatrixAccountMethod(addMatrixAccountMethod: AddMatrixAccountMethod)
    fun cancel()
    fun logoutFromProfile()
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

    final override val serverUrl =
        TextFieldViewModelImpl(maxLength = 1_000, get<MatrixMessengerConfiguration>().defaultHomeServer ?: "")

    private val config = get<MatrixMessengerConfiguration>()

    final override val serverDiscoveryState =
        serverUrl.debounce(1.seconds).transformLatest { serverUrl ->
            when {
                serverUrl.text.isBlank() -> emit(ServerDiscoveryState.None)
                else -> {
                    emit(ServerDiscoveryState.Loading)
                    serverUrl.text.serverDiscovery(
                        httpClientEngine = config.httpClientEngine,
                        httpClientConfig = config.httpClientConfig
                    ).onFailure {
                        log.debug(it) { "server discovery failed" }
                        emit(ServerDiscoveryState.Failure(i18n.serverDiscoveryFailed()))
                    }.onSuccess { serverDiscoveryUrl ->
                        val loginTypes = getLoginTypes(serverDiscoveryUrl)
                        if (loginTypes.isNotEmpty()) emit(ServerDiscoveryState.Success(loginTypes))
                        else emit(ServerDiscoveryState.Failure(i18n.serverDiscoveryFailed()))
                    }
                }
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), ServerDiscoveryState.None)

    override fun selectAddMatrixAccountMethod(addMatrixAccountMethod: AddMatrixAccountMethod) {
        onAddMatrixAccountMethod(addMatrixAccountMethod)
    }

    override fun cancel() {
        onCancel()
    }

    private suspend fun getLoginTypes(
        serverDiscoveryUrl: Url
    ): Set<AddMatrixAccountMethod> {
        MatrixClientServerApiClientImpl(
            serverDiscoveryUrl,
            httpClientEngine = config.httpClientEngine,
            httpClientConfig = config.httpClientConfig
        ).use { api ->
            val oAuth2LoginMethods = api.authentication.getOAuth2ServerMetadata()
                .fold(
                    onSuccess = { serverMetadata ->
                        buildSet {
                            add(
                                AddMatrixAccountMethod.OAuth2(
                                    serverUrl = serverDiscoveryUrl.toString(),
                                    type = OAuth2LoginViewModel.Type.LOGIN
                                )
                            )
                            if (serverMetadata.promptValuesSupported?.contains(PromptValue.Create) == true)
                                add(
                                    AddMatrixAccountMethod.OAuth2(
                                        serverUrl = serverDiscoveryUrl.toString(),
                                        type = OAuth2LoginViewModel.Type.REGISTER
                                    )
                                )
                        }
                    },
                    onFailure = {
                        log.info { "no oAuth2 login methods found, try to find classic ones" }
                        emptySet()
                    }
                )
            val classicLoginMethods = api.authentication.getLoginTypes()
                .fold(
                    onSuccess = { loginTypes ->
                        log.debug { "get login types succeeded" }
                        loginTypes.flatMap { loginType ->
                            when (loginType) {
                                is LoginType.Password ->
                                    setOf(AddMatrixAccountMethod.Password(serverDiscoveryUrl.toString()))

                                is LoginType.SSO ->
                                    loginType.identityProviders.map { identityProvider ->
                                        val icon = identityProvider.icon?.let {
                                            var byteArray: ByteArray? = null
                                            @Suppress("DEPRECATION") // Matrix does not support non-legacy for identity provider icons
                                            api.media.downloadLegacy(it) { media ->
                                                byteArray =
                                                    media.content.toByteArrayFlow()
                                                        .takeBytes(5 * 1024 * 1024) // max 5 MB
                                                        .toByteArray()
                                            }.onFailure { error ->
                                                log.warn { "could not download idp icon $error" }
                                            }.getOrNull()
                                            byteArray
                                        }
                                        AddMatrixAccountMethod.SSO(
                                            serverUrl = serverDiscoveryUrl.toString(),
                                            identityProvider = identityProvider,
                                            icon = icon,
                                        )
                                    }.ifEmpty {
                                        listOf(
                                            AddMatrixAccountMethod.SSO(
                                                serverUrl = serverDiscoveryUrl.toString(),
                                                identityProvider = null,
                                                icon = null,
                                            )
                                        )
                                    }

                                else -> setOf()
                            }
                        }.toSet()
                    },
                    onFailure = {
                        log.warn(it) { "no login methods found" }
                        emptySet()
                    }
                )
            val registerLoginMethod = api.authentication.register()
                .fold(
                    onSuccess = {
                        if (it is UIA.Step<*>)
                            setOf(AddMatrixAccountMethod.Register(serverDiscoveryUrl.toString()))
                        else emptySet()
                    },
                    onFailure = {
                        log.info { "no classic registration enabled" }
                        emptySet()
                    }
                )
            return oAuth2LoginMethods + classicLoginMethods + registerLoginMethod
        }
    }

    val profileManager = getOrNull<ProfileManager>()
    override val isMultiProfile: StateFlow<Boolean> =
        (profileManager?.isMultiProfileEnabled?.map { it == true } ?: flowOf(false)).stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(),
            false
        )

    override fun logoutFromProfile() {
        coroutineScope.launch {
            profileManager?.closeProfile()
        }
    }
}

class PreviewAddMatrixAccountViewModel : AddMatrixAccountViewModel {
    override val isFirstMatrixClient: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val serverUrl = TextFieldViewModelImpl(maxLength = 1_000, "matrix.org")
    override val serverDiscoveryState: MutableStateFlow<ServerDiscoveryState> =
        MutableStateFlow(ServerDiscoveryState.None)
    override val isMultiProfile: StateFlow<Boolean> = MutableStateFlow(false)
    override fun selectAddMatrixAccountMethod(addMatrixAccountMethod: AddMatrixAccountMethod) {
    }

    override fun cancel() {
    }

    override fun logoutFromProfile() {
    }
}
