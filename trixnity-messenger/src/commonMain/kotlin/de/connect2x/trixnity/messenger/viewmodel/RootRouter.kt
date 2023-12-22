package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.LoadStoreException
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.util.*
import de.connect2x.trixnity.messenger.viewmodel.connecting.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

class RootRouter(
    private val viewModelContext: ViewModelContext,
) {
    private val matrixClients = viewModelContext.get<MatrixClients>()
    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.MatrixClientInitialization,
        key = "RootRouter-${uuid4()}",
        childFactory = ::createChild,
    )

    private fun createChild(config: Config, componentContext: ComponentContext): RootWrapper {
        return when (config) {
            is Config.MatrixClientInitialization -> RootWrapper.MatrixClientInitialization(
                viewModelContext.get<MatrixClientInitializationViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        onNoAccounts = ::showAddMatrixAccount,
                        onInitializationSuccess = ::showMain,
                        onInitializationFailure = ::showAddMatrixAccount,
                        onStoreFailure = ::showStoreFailure,
                    )
            )

            is Config.RemoveMatrixAccount -> RootWrapper.MatrixClientLogout(
                viewModelContext.get<RemoveMatrixAccountViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        userId = config.userId,
                        onRemoveCompleted = ::showInitialization,
                    )
            )

            is Config.AddMatrixAccount -> RootWrapper.AddMatrixAccount(
                viewModelContext.get<AddMatrixAccountViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onAddMatrixAccountMethod = ::showAddMatrixAccountMethod,
                    onCancel = ::cancelAddMatrixAccount,
                )
            )

            is Config.PasswordLogin -> RootWrapper.PasswordLogin(
                viewModelContext.get<PasswordLoginViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    serverUrl = config.serverUrl,
                    onLogin = ::showMainOnLogin,
                    onBack = ::backToAddMatrixAccount,
                )
            )

            is Config.SSOLogin -> RootWrapper.SSOLogin(
                viewModelContext.get<SSOLoginViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    serverUrl = config.serverUrl,
                    providerId = config.providerId,
                    providerName = config.providerName,
                    onLogin = ::showMainOnLogin,
                    onBack = ::backToAddMatrixAccount,
                )
            )

            is Config.RegisterNewAccount -> RootWrapper.RegisterNewAccount(
                viewModelContext.get<RegisterNewAccountViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    serverUrl = config.serverUrl,
                    onLogin = ::showMainOnLogin,
                    onBack = ::backToAddMatrixAccount,
                )
            )

            is Config.Main -> {
                log.debug { "MatrixClients: $matrixClients" }
                RootWrapper.Main(
                    viewModelContext.get<MainViewModelFactory>().create(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        onCreateNewAccount = ::showAddMatrixAccount,
                        onRemoveAccount = ::showRemoveAccount,
                    ).apply { start() }
                )
            }

            is Config.StoreFailure -> RootWrapper.StoreFailure(
                viewModelContext.get<StoreFailureViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    userId = config.userId,
                    exception = config.exception,
                )
            )
        }
    }

    private fun showInitialization() {
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.MatrixClientInitialization)
    }

    private fun showMain() {
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.Main)
    }

    private fun showMainOnLogin() = viewModelContext.coroutineScope.launch {
        navigation.replaceAllSuspending(Config.Main)
        val instance = stack.value.active.instance
        if (instance is RootWrapper.Main) {
            instance.mainViewModel.closeAccountsOverview()
        }
    }

    private fun showAddMatrixAccount() {
        navigation.launchPush(viewModelContext.coroutineScope, Config.AddMatrixAccount)
    }

    private fun cancelAddMatrixAccount() = viewModelContext.coroutineScope.launch {
        if (matrixClients.value.isEmpty()) {
            log.info { "There are no MatrixClients configured yet, so close the app" }
            viewModelContext.getOrNull<CloseApp>()?.invoke()
        } else {
            navigation.popSuspending()
        }
    }


    private fun showAddMatrixAccountMethod(addMatrixAccountMethod: AddMatrixAccountMethod) {
        when (addMatrixAccountMethod) {
            is AddMatrixAccountMethod.Password -> navigation.launchPush(
                viewModelContext.coroutineScope,
                Config.PasswordLogin(addMatrixAccountMethod.serverUrl)
            )

            is AddMatrixAccountMethod.SSO -> navigation.launchPush(
                viewModelContext.coroutineScope,
                Config.SSOLogin(
                    serverUrl = addMatrixAccountMethod.serverUrl,
                    providerId = addMatrixAccountMethod.identityProvider.id,
                    providerName = addMatrixAccountMethod.identityProvider.name
                )
            )

            is AddMatrixAccountMethod.Register -> navigation.launchPush(
                viewModelContext.coroutineScope,
                Config.RegisterNewAccount(serverUrl = addMatrixAccountMethod.serverUrl)
            )
        }
    }

    private fun backToAddMatrixAccount() {
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun showStoreFailure(userId: UserId, exception: LoadStoreException) {
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.StoreFailure(userId, exception))
    }

    private fun showRemoveAccount(userId: UserId) {
        navigation.launchPush(viewModelContext.coroutineScope, Config.RemoveMatrixAccount(userId))
    }

    sealed class RootWrapper {
        data object None : RootWrapper()
        class MatrixClientInitialization(val matrixClientInitializationViewModel: MatrixClientInitializationViewModel) :
            RootWrapper()

        class MatrixClientLogout(val removeMatrixAccountViewModel: RemoveMatrixAccountViewModel) : RootWrapper()

        class Main(val mainViewModel: MainViewModel) : RootWrapper()
        class AddMatrixAccount(val addMatrixAccountViewModel: AddMatrixAccountViewModel) : RootWrapper()
        class PasswordLogin(val passwordLoginViewModel: PasswordLoginViewModel) : RootWrapper()
        class SSOLogin(val ssoLoginViewModel: SSOLoginViewModel) : RootWrapper()
        class RegisterNewAccount(val registerNewAccountViewModel: RegisterNewAccountViewModel) : RootWrapper()
        class StoreFailure(val storeFailureViewModel: StoreFailureViewModel) : RootWrapper()
    }

    @Serializable
    sealed class Config {
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
        data class SSOLogin(val serverUrl: String, val providerId: String, val providerName: String) : Config()

        @Serializable
        data class RegisterNewAccount(val serverUrl: String) : Config()

        @Serializable
        data class StoreFailure(
            val userId: UserId,
            val exception: LoadStoreException,
        ) : Config()
    }
}