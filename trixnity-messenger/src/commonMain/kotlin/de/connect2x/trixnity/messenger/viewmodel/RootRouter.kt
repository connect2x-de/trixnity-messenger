package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.GetAccountNames
import de.connect2x.trixnity.messenger.MatrixClientService
import de.connect2x.trixnity.messenger.NamedMatrixClients
import de.connect2x.trixnity.messenger.closeApp
import de.connect2x.trixnity.messenger.util.*
import de.connect2x.trixnity.messenger.viewmodel.connecting.*
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.io.async.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        initialConfiguration = Config.None,
        key = "RootRouter-${uuid4()}",
        childFactory = ::createChild,
    )

    private fun createChild(config: Config, componentContext: ComponentContext): RootWrapper {
        return when (config) {
            is Config.None -> RootWrapper.None

            is Config.MatrixClientInitialization -> RootWrapper.MatrixClientInitialization(
                viewModelContext.get<MatrixClientInitializationViewModelFactory>()
                    .newMatrixClientInitializationViewModel(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        matrixClientService = matrixClientService,
                        accountName = config.accountName,
                        onInitializationFailure = ::onInitializationFailure,
                        onStoreFailure = { config.accountName },
                    )
            )

            is Config.MatrixClientLogout -> RootWrapper.MatrixClientLogout(
                viewModelContext.get<MatrixClientLogoutViewModelFactory>()
                    .newMatrixClientLogoutViewModel(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        matrixClientService = matrixClientService,
                        accountName = config.accountName,
                        onLogoutCompleted = ::hideLogout,
                    )
            )

            is Config.AddMatrixAccount -> RootWrapper.AddMatrixAccount(
                viewModelContext.get<AddMatrixAccountViewModelFactory>().newAddMatrixAccountViewModel(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    matrixClientService = matrixClientService,
                    onLogin = ::onLogin,
                    onCancel = ::onCancelAddMatrixAccount,
                    onRegisterNewUser = ::showUserRegistration,
                )
            )

            is Config.RegisterNewAccount -> RootWrapper.RegisterNewAccount(
                viewModelContext.get<RegisterNewAccountViewModelFactory>().newRegisterNewAccountViewModel(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    matrixClientService = matrixClientService,
                    onLogin = ::userRegistrationSuccess,
                    onCancel = ::hideUserRegistration,
                )
            )

            is Config.Main -> {
                val matrixClients = matrixClientService.matrixClients.value
                matrixClients.forEach {
                    val matrixClient = it.matrixClient.value
                    requireNotNull(matrixClient)
                }

                log.debug { "MatrixClients: $matrixClients" }
                viewModelContext.getKoin().loadModules(
                    listOf(
                        module {
                            single { NamedMatrixClients(matrixClientService.matrixClients) }
                        })
                )
                RootWrapper.Main(
                    viewModelContext.get<MainViewModelFactory>().newMainViewModel(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        initialSyncOnceIsFinished = initialSyncOnceIsFinished,
                        minimizeMessenger = minimizeMessenger,
                        onCreateNewAccount = ::onCreateNewAccount,
                        onRemoveAccount = ::onRemoveAccountInternal,
                    ).apply { start() }
                )
            }

            is Config.StoreFailure -> RootWrapper.StoreFailure(
                viewModelContext.get<StoreFailureViewModelFactory>().newStoreFailureViewModel(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    accountName = config.accountName,
                    storeFailure = config.storeFailure,
                )
            )
        }
    }

    private fun onRemoveAccountInternal(accountName: String) {
        onRemoveAccount(accountName)
        if (stack.active.configuration is Config.AddMatrixAccount) {
            log.debug { "remove account $accountName -> close AddMatrixAccount view" }
            navigation.launchPop(viewModelContext.coroutineScope)
        }
    }

    suspend fun showMain() {
        log.debug { "show main view" }
        navigation.bringToFrontSuspending(Config.Main)
    }

    suspend fun showInitialization(accountName: String) {
        log.info { "Show initialization for $accountName" }
        navigation.bringToFrontSuspending(Config.MatrixClientInitialization(accountName))
    }

    fun onInitializationFailure() {
        log.info { "Authenticate" }
        navigation.launchBringToFront(viewModelContext.coroutineScope, Config.AddMatrixAccount)
    }

    suspend fun showFirstAccountCreation() {
        log.debug { "show first account creation view" }
        navigation.bringToFrontSuspending(Config.AddMatrixAccount)
    }

    private fun onCancelAddMatrixAccount() {
        log.debug { "cancel the creation of another MatrixClient" }

        CoroutineScope(Dispatchers.Main).launch {
            if (getAccountNames().isEmpty()) {
                log.info { "There are no MatrixClients configured yet, so close the app" }
                closeApp()
            } else {
                navigation.popSuspending()
            }
        }
    }

    private fun onCreateNewAccount() {
        navigation.launchBringToFront(viewModelContext.coroutineScope, Config.AddMatrixAccount)
    }

    private fun onLogin() {
        log.debug { "login: success" }
        if (stack.value.active.configuration is Config.AddMatrixAccount) {
            navigation.launchPop(viewModelContext.coroutineScope, onComplete = {
                val instance = stack.value.active.instance
                if (instance is RootWrapper.Main) {
                    instance.mainViewModel.closeAccountsOverview()
                }
            })
        }
    }

    fun selectFile(file: String) {
        val instance = stack.value.active.instance
        if (instance is RootWrapper.Main) {
            instance.mainViewModel.selectFile(file)
        }
    }

    fun dragFile(file: String) {
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

    fun showLogout(accountName: String) {
        navigation.launchPush(viewModelContext.coroutineScope, Config.MatrixClientLogout(accountName))
    }

    private fun hideLogout() {
        if (stack.active.configuration is Config.MatrixClientLogout) {
            navigation.launchPop(viewModelContext.coroutineScope)
        }
    }

    private fun showUserRegistration() {
        navigation.launchPush(viewModelContext.coroutineScope, Config.RegisterNewAccount)
    }

    private fun hideUserRegistration() {
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun userRegistrationSuccess() {
        navigation.launchPop(viewModelContext.coroutineScope) // close registration
        onLogin()
    }

    sealed class RootWrapper {
        object None : RootWrapper()
        class MatrixClientInitialization(val matrixClientInitializationViewModel: MatrixClientInitializationViewModel) :
            RootWrapper()

        class MatrixClientLogout(val matrixClientLogoutViewModel: MatrixClientLogoutViewModel) : RootWrapper()

        class Main(val mainViewModel: MainViewModel) : RootWrapper()
        class AddMatrixAccount(val addMatrixAccountViewModel: AddMatrixAccountViewModel) : RootWrapper()
        class RegisterNewAccount(val registerNewAccountViewModel: RegisterNewAccountViewModel): RootWrapper()
        class StoreFailure(val storeFailureViewModel: StoreFailureViewModel) : RootWrapper()
    }

    sealed class Config : Parcelable {
        @Parcelize
        object None : Config()

        @Parcelize
        object Main : Config()

        @Parcelize
        data class MatrixClientInitialization(val accountName: String) : Config()

        @Parcelize
        data class MatrixClientLogout(val accountName: String) : Config()

        @Parcelize
        object AddMatrixAccount : Config()

        @Parcelize
        object RegisterNewAccount: Config()

        @Parcelize
        data class StoreFailure(
            val accountName: String,
            val storeFailure: Result<Unit>
        ) : Config()
    }
}