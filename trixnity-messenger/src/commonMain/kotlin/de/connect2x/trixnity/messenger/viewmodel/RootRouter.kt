package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.*
import de.connect2x.trixnity.messenger.util.*
import de.connect2x.trixnity.messenger.viewmodel.connecting.*
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.FileDescriptor
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.io.async.launch
import org.koin.core.component.get
import org.koin.dsl.module

private val log = KotlinLogging.logger { }

class RootRouter(
    private val viewModelContext: ViewModelContext,
    private val matrixClientService: MatrixClientService,
    private val initialSyncOnceIsFinished: (Boolean) -> Unit,
    private val onRemoveAccount: (String) -> Unit,
    private val minimizeMessenger: () -> Unit,
) {
    private val getAccountNames = viewModelContext.get<GetAccountNames>()
    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        initialConfiguration = Config.MatrixClientInitialization,
        key = "RootRouter-${uuid4()}",
        childFactory = ::createChild,
    )

    private fun createChild(config: Config, componentContext: ComponentContext): RootWrapper {
        return when (config) {
            is Config.MatrixClientInitialization -> RootWrapper.MatrixClientInitialization(
                viewModelContext.get<MatrixClientInitializationViewModelFactory>()
                    .newMatrixClientInitializationViewModel(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        matrixClientService = matrixClientService,
                        onNoAccounts = ::showAddMatrixAccount,
                        onInitializationSuccess = ::showMain,
                        onInitializationFailure = ::showAddMatrixAccount,
                        onStoreFailure = ::showStoreFailure,
                    )
            )

            is Config.MatrixClientLogout -> RootWrapper.MatrixClientLogout(
                viewModelContext.get<MatrixClientLogoutViewModelFactory>()
                    .newMatrixClientLogoutViewModel(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        matrixClientService = matrixClientService,
                        accountName = config.accountName,
                        onLogoutCompleted = ::showInitialization,
                    )
            )

            is Config.AddMatrixAccount -> RootWrapper.AddMatrixAccount(
                viewModelContext.get<AddMatrixAccountViewModelFactory>().newAddMatrixAccountViewModel(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onAddMatrixAccountMethod = ::showAddMatrixAccountMethod,
                    onCancel = ::cancelAddMatrixAccount,
                )
            )

            is Config.PasswordLogin -> RootWrapper.PasswordLogin(
                viewModelContext.get<PasswordLoginViewModelFactory>().newPasswordLoginViewModel(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    serverUrl = config.serverUrl,
                    matrixClientService = matrixClientService,
                    onLogin = ::showMainOnLogin,
                    onBack = ::backToAddMatrixAccount,
                )
            )

            is Config.SSOLogin -> RootWrapper.SSOLogin(
                viewModelContext.get<SSOLoginViewModelFactory>().newSSOLoginViewModel(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    serverUrl = config.serverUrl,
                    matrixClientService = matrixClientService,
                    providerId = config.providerId,
                    providerName = config.providerName,
                    onLogin = ::showMainOnLogin,
                    onBack = ::backToAddMatrixAccount,
                )
            )

            is Config.RegisterNewAccount -> RootWrapper.RegisterNewAccount(
                viewModelContext.get<RegisterNewAccountViewModelFactory>().newRegisterNewAccountViewModel(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    serverUrl = config.serverUrl,
                    matrixClientService = matrixClientService,
                    onLogin = ::showMainOnLogin,
                    onBack = ::backToAddMatrixAccount,
                )
            )

            is Config.Main -> {
                val matrixClients = matrixClientService.matrixClients.value
                matrixClients.forEach {
                    val matrixClient = it.matrixClient.value
                    checkNotNull(matrixClient) { "matrixClient ${it.accountName} missing" }
                }

                log.debug { "MatrixClients: $matrixClients" }
                viewModelContext.getKoin().loadModules(
                    listOf(
                        module {
                            single { NamedMatrixClients(matrixClientService.matrixClients) }
                        }
                    )
                )
                RootWrapper.Main(
                    viewModelContext.get<MainViewModelFactory>().newMainViewModel(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        initialSyncOnceIsFinished = initialSyncOnceIsFinished,
                        minimizeMessenger = minimizeMessenger,
                        onCreateNewAccount = ::showAddMatrixAccount,
                        onRemoveAccount = onRemoveAccount,
                    ).apply { start() }
                )
            }

            is Config.StoreFailure -> RootWrapper.StoreFailure(
                viewModelContext.get<StoreFailureViewModelFactory>().newStoreFailureViewModel(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    accountName = config.accountName,
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
        if (getAccountNames().isEmpty()) {
            log.info { "There are no MatrixClients configured yet, so close the app" }
            closeApp()
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

    private fun showStoreFailure(accountName: String, exception: LoadStoreException) {
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.StoreFailure(accountName, exception))
    }

    fun showLogout(accountName: String) {
        navigation.launchPush(viewModelContext.coroutineScope, Config.MatrixClientLogout(accountName))
    }

    fun selectFile(file: FileDescriptor) {
        val instance = stack.value.active.instance
        if (instance is RootWrapper.Main) {
            instance.mainViewModel.selectFile(file)
        }
    }

    fun dragFile(file: FileDescriptor) {
        val instance = stack.value.active.instance
        if (instance is RootWrapper.Main) {
            instance.mainViewModel.dragFile(file)
        }
    }

    fun dragFileExit() {
        val instance = stack.value.active.instance
        if (instance is RootWrapper.Main) {
            instance.mainViewModel.dragFileExit()
        }
    }

    sealed class RootWrapper {
        object None : RootWrapper()
        class MatrixClientInitialization(val matrixClientInitializationViewModel: MatrixClientInitializationViewModel) :
            RootWrapper()

        class MatrixClientLogout(val matrixClientLogoutViewModel: MatrixClientLogoutViewModel) : RootWrapper()

        class Main(val mainViewModel: MainViewModel) : RootWrapper()
        class AddMatrixAccount(val addMatrixAccountViewModel: AddMatrixAccountViewModel) : RootWrapper()
        class PasswordLogin(val passwordLoginViewModel: PasswordLoginViewModel) : RootWrapper()
        class SSOLogin(val ssoLoginViewModel: SSOLoginViewModel) : RootWrapper()
        class RegisterNewAccount(val registerNewAccountViewModel: RegisterNewAccountViewModel) : RootWrapper()
        class StoreFailure(val storeFailureViewModel: StoreFailureViewModel) : RootWrapper()
    }

    sealed class Config : Parcelable {
        @Parcelize
        object MatrixClientInitialization : Config()

        @Parcelize
        object Main : Config()

        @Parcelize
        data class MatrixClientLogout(val accountName: String) : Config()

        @Parcelize
        object AddMatrixAccount : Config()

        @Parcelize
        data class PasswordLogin(val serverUrl: String) : Config()

        @Parcelize
        data class SSOLogin(val serverUrl: String, val providerId: String, val providerName: String) : Config()

        @Parcelize
        data class RegisterNewAccount(val serverUrl: String) : Config()

        @Parcelize
        data class StoreFailure(
            val accountName: String,
            val exception: @RawValue LoadStoreException,
        ) : Config()
    }
}