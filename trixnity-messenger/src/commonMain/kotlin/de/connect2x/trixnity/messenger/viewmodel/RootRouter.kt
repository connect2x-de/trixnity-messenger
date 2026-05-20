package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.clientserverapi.client.oauth2.OAuth2LoginFlow
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClientInitializationException
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.util.CloseApp
import de.connect2x.trixnity.messenger.util.UriHandler
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.util.launchNavigate
import de.connect2x.trixnity.messenger.util.launchPop
import de.connect2x.trixnity.messenger.util.launchPush
import de.connect2x.trixnity.messenger.util.launchReplaceAll
import de.connect2x.trixnity.messenger.util.replaceAllSuspending
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountMethod
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationFailureViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationFailureViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2LoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2LoginViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterMatrixAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.RemoveMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.RemoveMatrixAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOLoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOLoginViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.util.toFlow
import io.ktor.http.Url
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.component.get

class RootRouter(private val viewModelContext: ViewModelContext) {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.viewmodel.RootRouter")
    }

    private val matrixClients = viewModelContext.get<MatrixClients>()
    private val settings = viewModelContext.get<MatrixMessengerSettingsHolder>()
    private val messengerConfiguration = viewModelContext.get<MatrixMessengerConfiguration>()
    private val uriHandler = viewModelContext.get<UriHandler>()

    private val navigation = StackNavigation<Config>()
    val stack =
        viewModelContext.childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.None,
            key = "RootRouter-${Uuid.random()}",
            childFactory = ::createChild,
        )

    private fun createChild(config: Config, componentContext: ComponentContext): Wrapper {
        return when (config) {
            is Config.None -> Wrapper.None
            is Config.MatrixClientInitialization ->
                Wrapper.MatrixClientInitialization(
                    viewModelContext
                        .get<MatrixClientInitializationViewModelFactory>()
                        .create(
                            viewModelContext =
                                viewModelContext.childContext("MatrixClientInitialization", componentContext),
                            onNoAccounts = ::showAddMatrixAccount,
                            onInitializationSuccess = ::showMain,
                            onInitializationFailure = ::onInitializationFailure,
                        )
                )

            is Config.AddMatrixAccount ->
                Wrapper.AddMatrixAccount(
                    viewModelContext
                        .get<AddMatrixAccountViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext("AddMatrixAccount", componentContext),
                            onAddMatrixAccountMethod = ::showAddMatrixAccountMethod,
                            onCancel = ::cancelAddMatrixAccount,
                        )
                )

            is Config.OAuth2Login ->
                Wrapper.OAuth2Login(
                    viewModelContext
                        .get<OAuth2LoginViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext("OAuth2Login", componentContext),
                            type = config.type,
                            serverUrl = config.serverUrl,
                            initialState = config.initialState,
                            onLogin = ::showMainOnLogin,
                            onBack = ::backToAddMatrixAccount,
                        )
                )

            is Config.RegisterMatrixAccount ->
                Wrapper.RegisterMatrixAccount(
                    viewModelContext
                        .get<RegisterMatrixAccountViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext("RegisterMatrixAccount", componentContext),
                            serverUrl = config.serverUrl,
                            onLogin = ::showMainOnLogin,
                            onBack = ::backToAddMatrixAccount,
                        )
                )

            is Config.RemoveMatrixAccount ->
                Wrapper.RemoveMatrixAccount(
                    viewModelContext
                        .get<RemoveMatrixAccountViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext("RemoveMatrixAccount", componentContext),
                            userId = config.userId,
                            onRemoveCompleted = {}, // do nothing as the MainViewModel will show a sync
                        )
                )

            is Config.PasswordLogin ->
                Wrapper.PasswordLogin(
                    viewModelContext
                        .get<PasswordLoginViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext("PasswordLogin", componentContext),
                            serverUrl = config.serverUrl,
                            onLogin = ::showMainOnLogin,
                            onBack = ::backToAddMatrixAccount,
                        )
                )

            is Config.SSOLogin ->
                Wrapper.SSOLogin(
                    viewModelContext
                        .get<SSOLoginViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext("SSOLogin", componentContext),
                            serverUrl = config.serverUrl,
                            providerId = config.providerId,
                            providerName = config.providerName,
                            initialState = config.initialState,
                            onLogin = ::showMainOnLogin,
                            onBack = ::backToAddMatrixAccount,
                        )
                )

            is Config.Main -> {
                log.debug { "MatrixClients: $matrixClients" }
                Wrapper.Main(
                    viewModelContext
                        .get<MainViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext("Main", componentContext),
                            onCreateNewAccount = ::showAddMatrixAccount,
                            onRemoveAccount = ::showRemoveAccount,
                        )
                        .apply { start() }
                )
            }

            is Config.MatrixClientInitializationFailure ->
                Wrapper.MatrixClientInitializationFailure(
                    viewModelContext
                        .get<MatrixClientInitializationFailureViewModelFactory>()
                        .create(
                            viewModelContext =
                                viewModelContext.childContext("MatrixClientInitializationFailure", componentContext),
                            userId = config.userId,
                            exception = config.exception,
                            onDeletionFinished = ::showInitialization,
                        )
                )
        }
    }

    init {
        log.debug { "init RootViewModel" }
        // when a MatrixClient is removed, be sure to re-init everything (e.g. show root on zero MatrixClients)
        viewModelContext.coroutineScope.launch {
            matrixClients
                .scan(0 to 0) { old, new -> old.second to new.size }
                .collect { (old, new) ->
                    log.debug { "show initialization? MatrixClients old: $old, new: $new" }
                    if (new < old) showInitialization()
                }
        }
    }

    /**
     * Use parameter [force] to show the initialization regardless of the current config. If false, the initialization
     * is only shown when the current stack is empty.
     */
    fun showInitialization(force: Boolean = true) {
        log.debug { "showInitialization" }
        navigation.launchNavigate(viewModelContext.coroutineScope) {
            if (it == listOf(Config.None) || force) {
                listOf(Config.MatrixClientInitialization)
            } else it
        }
    }

    private fun showMain() {
        log.debug { "show main" }
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.Main)
    }

    private fun showMainOnLogin() =
        viewModelContext.coroutineScope.launch {
            log.debug { "showMainOnLogin" }
            navigation.replaceAllSuspending(Config.Main)
        }

    private fun showAddMatrixAccount() {
        navigation.launchNavigate(viewModelContext.coroutineScope) {
            if (it.contains(Config.AddMatrixAccount)) it
            else {
                log.debug { "showAddMatrixAccount" }
                listOf(Config.AddMatrixAccount)
            }
        }
    }

    private fun cancelAddMatrixAccount() =
        viewModelContext.coroutineScope.launch {
            log.debug { "cancelAddMatrixAccount" }

            val profileManager = viewModelContext.get<ProfileManager>()
            val isMultiProfile = profileManager.isMultiProfileEnabled.value
            when {
                matrixClients.value.isNotEmpty() -> {
                    navigation.replaceAllSuspending(Config.MatrixClientInitialization)
                }

                isMultiProfile == true -> {
                    profileManager.closeProfile()
                }

                else -> {
                    log.info { "There are no MatrixClients configured yet, so close the app" }
                    viewModelContext.getOrNull<CloseApp>()?.invoke()
                }
            }
        }

    private fun showAddMatrixAccountMethod(addMatrixAccountMethod: AddMatrixAccountMethod) {
        log.debug { "showAddMatrixAccountMethod: $addMatrixAccountMethod" }
        when (addMatrixAccountMethod) {
            is AddMatrixAccountMethod.OAuth2 ->
                navigation.launchPush(
                    viewModelContext.coroutineScope,
                    Config.OAuth2Login(addMatrixAccountMethod.serverUrl, addMatrixAccountMethod.type),
                )

            is AddMatrixAccountMethod.Password ->
                navigation.launchPush(
                    viewModelContext.coroutineScope,
                    Config.PasswordLogin(addMatrixAccountMethod.serverUrl),
                )

            is AddMatrixAccountMethod.SSO ->
                navigation.launchPush(
                    viewModelContext.coroutineScope,
                    Config.SSOLogin(
                        serverUrl = addMatrixAccountMethod.serverUrl,
                        providerId = addMatrixAccountMethod.identityProvider?.id,
                        providerName = addMatrixAccountMethod.identityProvider?.name,
                    ),
                )

            is AddMatrixAccountMethod.Register ->
                navigation.launchPush(
                    viewModelContext.coroutineScope,
                    Config.RegisterMatrixAccount(serverUrl = addMatrixAccountMethod.serverUrl),
                )
        }
    }

    private fun backToAddMatrixAccount() {
        log.debug { "backToAddMatrixAccount" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onInitializationFailure(userId: UserId, exception: MatrixClientInitializationException) {
        log.debug { "showStoreFailure" }
        navigation.launchReplaceAll(
            viewModelContext.coroutineScope,
            Config.MatrixClientInitializationFailure(userId, exception),
        )
    }

    private fun showRemoveAccount(userId: UserId) {
        log.debug { "showRemoveAccount" }
        // replace the Config.Main to remove the MainViewModel which can trigger some actions we do not want for logged
        // out clients
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.RemoveMatrixAccount(userId))
    }

    private suspend fun resumeSsoLogin(redirectUri: String) {
        log.debug { "requested resume sso login" }
        val state = settings.value.base.ssoLoginState
        if (state != null) {
            log.debug { "wait for MatrixClientInitialization before resuming sso login" }
            stack.toFlow().first {
                it.active.configuration != Config.None && it.active.configuration != Config.MatrixClientInitialization
            }
            log.info { "resume sso login" }
            navigation.replaceAllSuspending(
                Config.AddMatrixAccount,
                Config.SSOLogin(state.serverUrl, state.providerId, state.providerName, state.state),
            )
            val instance = stack.value.active.instance
            if (instance is Wrapper.SSOLogin) {
                instance.viewModel.resumeLogin(redirectUri)
            }
        } else {
            log.warn { "cannot resume sso login (no state saved)" }
        }
    }

    private suspend fun resumeOAuth2Login(redirectUri: String) {
        log.debug { "requested resume oAuth2 login" }
        val state = settings.value.base.oAuth2LoginState
        if (state != null) {
            log.debug { "wait for MatrixClientInitialization before resuming oAuth2 login" }
            stack.toFlow().first {
                it.active.configuration != Config.None && it.active.configuration != Config.MatrixClientInitialization
            }
            log.info { "resume oAuth2 login" }
            val instance = stack.value.active.instance
            if (instance is Wrapper.OAuth2Login) {
                instance.viewModel.resumeLogin(redirectUri)
            } else {
                navigation.replaceAllSuspending(
                    Config.AddMatrixAccount,
                    Config.OAuth2Login(state.serverUrl, state.type, state.state),
                )
                (stack.value.active.instance as? Wrapper.OAuth2Login)?.viewModel?.resumeLogin(redirectUri)
            }
        } else {
            log.warn { "cannot resume sso login (no state saved)" }
        }
    }

    init {
        viewModelContext.coroutineScope.launch {
            uriHandler.collectLatest { uri ->
                if (Url(uri).encodedPath == "/${messengerConfiguration.appUriSsoRedirect}") {
                    resumeSsoLogin(uri)
                }
                if (Url(uri).encodedPath == "/${messengerConfiguration.appUriOAuth2Redirect}") {
                    resumeOAuth2Login(uri)
                }
            }
        }
    }

    sealed class Wrapper {
        data object None : Wrapper()

        class MatrixClientInitialization(val viewModel: MatrixClientInitializationViewModel) : Wrapper()

        class MatrixClientInitializationFailure(val viewModel: MatrixClientInitializationFailureViewModel) : Wrapper()

        class Main(val viewModel: MainViewModel) : Wrapper()

        class AddMatrixAccount(val viewModel: AddMatrixAccountViewModel) : Wrapper()

        class OAuth2Login(val viewModel: OAuth2LoginViewModel) : Wrapper()

        class RegisterMatrixAccount(val viewModel: RegisterMatrixAccountViewModel) : Wrapper()

        class RemoveMatrixAccount(val viewModel: RemoveMatrixAccountViewModel) : Wrapper()

        class PasswordLogin(val viewModel: PasswordLoginViewModel) : Wrapper()

        class SSOLogin(val viewModel: SSOLoginViewModel) : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable data object None : Config()

        @Serializable data object MatrixClientInitialization : Config()

        @Serializable data object Main : Config()

        @Serializable data class RemoveMatrixAccount(val userId: UserId) : Config()

        @Serializable data object AddMatrixAccount : Config()

        @Serializable
        data class OAuth2Login(
            val serverUrl: String,
            val type: OAuth2LoginViewModel.Type,
            val initialState: OAuth2LoginFlow.AuthRequestData.State? = null,
        ) : Config()

        @Serializable data class PasswordLogin(val serverUrl: String) : Config()

        @Serializable
        data class SSOLogin(
            val serverUrl: String,
            val providerId: String?,
            val providerName: String?,
            val initialState: String? = null,
        ) : Config()

        @Serializable data class RegisterMatrixAccount(val serverUrl: String) : Config()

        @Serializable
        data class MatrixClientInitializationFailure(
            val userId: UserId,
            val exception: MatrixClientInitializationException,
        ) : Config()
    }
}
