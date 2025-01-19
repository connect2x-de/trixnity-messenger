package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.LoadStoreException
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.CloseApp
import de.connect2x.trixnity.messenger.util.UrlHandler
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.util.launchPop
import de.connect2x.trixnity.messenger.util.launchPush
import de.connect2x.trixnity.messenger.util.launchReplaceAll
import de.connect2x.trixnity.messenger.util.replaceAllSuspending
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountMethod
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterNewAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterNewAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.RemoveMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.RemoveMatrixAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOLoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOLoginViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.StoreFailureViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.StoreFailureViewModelFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

class RootRouter(
    private val viewModelContext: ViewModelContext,
) {
    private val matrixClients = viewModelContext.get<MatrixClients>()
    private val settings = viewModelContext.get<MatrixMessengerSettingsHolder>()
    private val messengerConfiguration = viewModelContext.get<MatrixMessengerConfiguration>()
    private val urlHandler = viewModelContext.get<UrlHandler>()

    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.None,
        key = "RootRouter-${uuid4()}",
        childFactory = ::createChild,
    )

    private fun createChild(config: Config, componentContext: ComponentContext): Wrapper {
        return when (config) {
            is Config.None -> Wrapper.None
            is Config.MatrixClientInitialization -> Wrapper.MatrixClientInitialization(
                viewModelContext.get<MatrixClientInitializationViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        onNoAccounts = ::showAddMatrixAccount,
                        onInitializationSuccess = ::showMain,
                        onInitializationFailure = ::showAddMatrixAccount,
                        onStoreFailure = ::showStoreFailure,
                    )
            )

            is Config.RemoveMatrixAccount -> Wrapper.MatrixClientLogout(
                viewModelContext.get<RemoveMatrixAccountViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        userId = config.userId,
                        onRemoveCompleted = { }, // do nothing as the MainViewModel will show a sync
                    )
            )

            is Config.AddMatrixAccount -> Wrapper.AddMatrixAccount(
                viewModelContext.get<AddMatrixAccountViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onAddMatrixAccountMethod = ::showAddMatrixAccountMethod,
                    onCancel = ::cancelAddMatrixAccount,
                )
            )

            is Config.PasswordLogin -> Wrapper.PasswordLogin(
                viewModelContext.get<PasswordLoginViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    serverUrl = config.serverUrl,
                    onLogin = ::showMainOnLogin,
                    onBack = ::backToAddMatrixAccount,
                )
            )

            is Config.SSOLogin -> Wrapper.SSOLogin(
                viewModelContext.get<SSOLoginViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    serverUrl = config.serverUrl,
                    providerId = config.providerId,
                    providerName = config.providerName,
                    initialState = config.initialState,
                    onLogin = ::showMainOnLogin,
                    onBack = ::backToAddMatrixAccount
                )
            )

            is Config.RegisterNewAccount -> Wrapper.RegisterNewAccount(
                viewModelContext.get<RegisterNewAccountViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    serverUrl = config.serverUrl,
                    onLogin = ::showMainOnLogin,
                    onBack = ::backToAddMatrixAccount,
                )
            )

            is Config.Main -> {
                log.debug { "MatrixClients: $matrixClients" }
                Wrapper.Main(
                    viewModelContext.get<MainViewModelFactory>().create(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        onCreateNewAccount = ::showAddMatrixAccount,
                        onRemoveAccount = ::showRemoveAccount,
                    ).apply { start() }
                )
            }

            is Config.StoreFailure -> Wrapper.StoreFailure(
                viewModelContext.get<StoreFailureViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
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
            matrixClients.scan(0 to 0) { old, new ->
                old.second to new.size
            }.collect { (old, new) ->
                log.debug { "show initialization? MatrixClients old: $old, new: $new" }
                if (new < old) showInitialization()
            }
        }
    }

    fun showNone() {
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.None)
    }

    fun showInitialization() {
        log.debug { "showInitialization" }
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.MatrixClientInitialization)
    }

    private fun showMain() {
        log.debug { "show main" }
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.Main)
    }

    private fun showMainOnLogin() = viewModelContext.coroutineScope.launch {
        log.debug { "showMainOnLogin" }
        navigation.replaceAllSuspending(Config.Main)
        val instance = stack.value.active.instance
        if (instance is Wrapper.Main) {
            instance.viewModel.closeAccountsOverview()
        }
    }

    private fun showAddMatrixAccount() {
        log.debug { "showAddMatrixAccount" }
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.AddMatrixAccount)
    }

    private fun cancelAddMatrixAccount() = viewModelContext.coroutineScope.launch {
        if (matrixClients.value.isEmpty()) {
            log.info { "There are no MatrixClients configured yet, so close the app" }
            viewModelContext.getOrNull<CloseApp>()?.invoke()
        } else {
            navigation.replaceAllSuspending(Config.MatrixClientInitialization)
        }
    }


    private fun showAddMatrixAccountMethod(addMatrixAccountMethod: AddMatrixAccountMethod) {
        log.debug { "showAddMatrixAccountMethod: $addMatrixAccountMethod" }
        when (addMatrixAccountMethod) {
            is AddMatrixAccountMethod.Password -> navigation.launchPush(
                viewModelContext.coroutineScope,
                Config.PasswordLogin(addMatrixAccountMethod.serverUrl)
            )

            is AddMatrixAccountMethod.SSO -> navigation.launchPush(
                viewModelContext.coroutineScope,
                Config.SSOLogin(
                    serverUrl = addMatrixAccountMethod.serverUrl,
                    providerId = addMatrixAccountMethod.identityProvider?.id,
                    providerName = addMatrixAccountMethod.identityProvider?.name
                )
            )

            is AddMatrixAccountMethod.Register -> navigation.launchPush(
                viewModelContext.coroutineScope,
                Config.RegisterNewAccount(serverUrl = addMatrixAccountMethod.serverUrl)
            )
        }
    }

    private fun backToAddMatrixAccount() {
        log.debug { "backToAddMatrixAccount" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun showStoreFailure(userId: UserId, exception: LoadStoreException) {
        log.debug { "showStoreFailure" }
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.StoreFailure(userId, exception))
    }

    private fun showRemoveAccount(userId: UserId) {
        log.debug { "showRemoveAccount" }
        // replace the Config.Main to remove the MainViewModel which can trigger some actions we do not want for logged out clients
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.RemoveMatrixAccount(userId))
    }

    private suspend fun resumeSsoLogin(redirectUrl: Url) {
        log.debug { "resumeSsoLogin" }
        val state = settings.value.base.ssoState
        if (state != null) {
            log.info { "resume sso login" }
            navigation.replaceAllSuspending(
                Config.AddMatrixAccount,
                Config.SSOLogin(state.serverUrl, state.providerId, state.providerName, state.state),
            )
            val instance = stack.value.active.instance
            if (instance is Wrapper.SSOLogin) {
                instance.viewModel.resumeLogin(redirectUrl)
            }
        } else {
            log.warn { "cannot resume sso login" }
        }
    }

    init {
        viewModelContext.coroutineScope.launch {
            urlHandler.collectLatest { url ->
                if (url.encodedPath == "/${messengerConfiguration.ssoRedirectPath}")
                    resumeSsoLogin(url)
            }
        }
    }

    sealed class Wrapper {
        data object None : Wrapper()
        class MatrixClientInitialization(val viewModel: MatrixClientInitializationViewModel) :
            Wrapper()

        class MatrixClientLogout(val viewModel: RemoveMatrixAccountViewModel) : Wrapper()

        class Main(val viewModel: MainViewModel) : Wrapper()
        class AddMatrixAccount(val viewModel: AddMatrixAccountViewModel) : Wrapper()
        class PasswordLogin(val viewModel: PasswordLoginViewModel) : Wrapper()
        class SSOLogin(val viewModel: SSOLoginViewModel) : Wrapper()
        class RegisterNewAccount(val viewModel: RegisterNewAccountViewModel) : Wrapper()
        class StoreFailure(val viewModel: StoreFailureViewModel) : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object None : Config()

        @Serializable
        data object MatrixClientInitialization : Config()

        @Serializable
        data object Main : Config()

        @Serializable
        data class RemoveMatrixAccount(val userId: UserId) : Config()

        @Serializable
        data object AddMatrixAccount : Config()

        @Serializable
        data class PasswordLogin(val serverUrl: String) : Config()

        @Serializable
        data class SSOLogin(
            val serverUrl: String,
            val providerId: String?,
            val providerName: String?,
            val initialState: String? = null,
        ) : Config()

        @Serializable
        data class RegisterNewAccount(val serverUrl: String) : Config()

        @Serializable
        data class StoreFailure(
            val userId: UserId,
            val exception: LoadStoreException,
        ) : Config()
    }
}
