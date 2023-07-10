package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.GetAccountNames
import de.connect2x.trixnity.messenger.MatrixClientService
import de.connect2x.trixnity.messenger.viewmodel.util.coroutineScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.koin.core.Koin
import org.koin.core.KoinApplication
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger { }

interface RootViewModel {
    /**
     * Can be used to get/inject singletons outside of the RootViewModel.
     */
    val koin: Koin
    val rootStack: Value<ChildStack<RootRouter.Config, RootRouter.RootWrapper>>

    /**
     * Used for DnD on Desktop: a file is dropped onto the messenger view
     */
    fun selectFile(file: String)

    /**
     * Used for DnD on Desktop: file is dragged into the messenger view
     */
    fun dragFile(file: String)

    /**
     * Used for DnD on Desktop: a file is no longer dragged above the messenger view
     */
    fun dragFileExit()

    fun removeAccount(accountName: String)
}


open class RootViewModelImpl(
    componentContext: ComponentContext,
    private val matrixClientService: MatrixClientService,
    initialSyncOnceIsFinished: (Boolean) -> Unit,
    koinApplication: KoinApplication,
    minimizeMessenger: () -> Unit = {},
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : ComponentContext by componentContext, RootViewModel {

    protected val scope: CoroutineScope = coroutineScope(coroutineContext)

    /**
     * Can be used to get/inject singletons outside of the RootViewModel.
     */
    override val koin = koinApplication.koin

    private val router = RootRouter(
        viewModelContext = ViewModelContextImpl(koin, componentContext),
        matrixClientService = matrixClientService,
        initialSyncOnceIsFinished = initialSyncOnceIsFinished,
        onRemoveAccount = ::onRemoveAccount,
        minimizeMessenger = minimizeMessenger,
    )
    override val rootStack: Value<ChildStack<RootRouter.Config, RootRouter.RootWrapper>> = router.stack

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            init(koinApplication)
        }
    }

    private suspend fun init(
        koinApplication: KoinApplication
    ) {
        val accounts = koinApplication.koin.get<GetAccountNames>()()
        log.debug { "accounts: $accounts" }
        if (accounts.isEmpty()) { // no messenger login defined yet, show account creation
            router.showFirstAccountCreation()
            initMatrixClient(
                matrixClientService.matrixClients.first { it.isNotEmpty() }.first().accountName
            )
        } else {
            accounts.forEach { accountName ->
                log.debug { "init MatrixClient for account '$accountName'" }
                val namedMatrixClient =
                    matrixClientService.matrixClients.value.find { it.accountName == accountName }
                if (namedMatrixClient == null) {
                    initMatrixClient(accountName)
                } else {
                    log.warn { "found an existing MatrixClient for account '$accountName': $namedMatrixClient" }
                }
            }
        }
        router.showMain()

        matrixClientService.matrixClients.first { namedMatrixClients ->
            namedMatrixClients.isEmpty()
        }
        init(koinApplication)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun initMatrixClient(
        accountName: String
    ) {
        router.showInitialization(accountName)
        log.info { "wait for MatrixClient of '$accountName' to be initialized" }
        matrixClientService.matrixClients.flatMapLatest { namedMatrixClients ->
            combine(
                namedMatrixClients.map { namedMatrixClient ->
                    namedMatrixClient.matrixClient.map { if (it == null) null else namedMatrixClient.accountName }
                }
            ) {
                it
            }
        }.first { accountNames ->
            accountNames.any { foundAccountName ->
                foundAccountName == accountName
            }
        }
        log.info { "MatrixClient '$accountName' initialized" }
    }

    override fun removeAccount(accountName: String) {
        onRemoveAccount(accountName)
    }

    private fun onRemoveAccount(accountName: String) {
        router.showLogout(accountName)
    }

    override fun selectFile(file: String) {
        router.selectFile(file)
    }

    override fun dragFile(file: String) {
        router.dragFile(file)
    }

    override fun dragFileExit() {
        router.dragFileExit()
    }
}