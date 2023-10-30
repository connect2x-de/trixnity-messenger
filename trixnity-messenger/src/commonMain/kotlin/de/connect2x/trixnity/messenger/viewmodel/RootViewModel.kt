package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.DefaultMatrixClientService
import de.connect2x.trixnity.messenger.MatrixClientService
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.util.coroutineScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
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
    fun selectFile(file: FileDescriptor)

    /**
     * Used for DnD on Desktop: file is dragged into the messenger view
     */
    fun dragFile(file: FileDescriptor)

    /**
     * Used for DnD on Desktop: a file is no longer dragged above the messenger view
     */
    fun dragFileExit()

    fun removeAccount(accountName: String)
}


open class RootViewModelImpl(
    componentContext: ComponentContext = DefaultComponentContext(LifecycleRegistry()),
    koinApplication: KoinApplication = koinApplication {
        modules(trixnityMessengerModule())
    },
    matrixClientService: MatrixClientService = DefaultMatrixClientService(koinApplication.koin),
    initialSyncOnceIsFinished: (Boolean) -> Unit = {},
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

    override fun removeAccount(accountName: String) {
        onRemoveAccount(accountName)
    }

    private fun onRemoveAccount(accountName: String) {
        router.showLogout(accountName)
    }

    override fun selectFile(file: FileDescriptor) {
        router.selectFile(file)
    }

    override fun dragFile(file: FileDescriptor) {
        router.dragFile(file)
    }

    override fun dragFileExit() {
        router.dragFileExit()
    }

    // for iOS, since default parameters do not work there
    companion object {
        fun create(
            koinApplication: KoinApplication
        ): RootViewModel =
            RootViewModelImpl(koinApplication = koinApplication)
    }
}