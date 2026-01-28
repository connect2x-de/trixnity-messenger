package de.connect2x.trixnity.messenger.viewmodel.initialsync

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.util.launchReplaceCurrent
import de.connect2x.trixnity.messenger.util.popSuspending
import de.connect2x.trixnity.messenger.util.replaceCurrentSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.serialization.Serializable
import org.koin.core.component.get

class InitialSyncRouter(
    private val viewModelContext: ViewModelContext,
) {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncRouter")
    }

    private val initialSyncNavigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = initialSyncNavigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Undefined, // we do not yet know whether an initial sync is needed
        key = "initialSyncRouter",
        handleBackButton = false,
        childFactory = ::createInitialSyncChild,
    )

    private fun createInitialSyncChild(
        initialSyncConfig: Config,
        componentContext: ComponentContext,
    ): Wrapper =
        when (initialSyncConfig) {
            is Config.None -> Wrapper.None
            is Config.Undefined -> Wrapper.Undefined
            is Config.Sync -> Wrapper.Sync(
                viewModelContext.get<SyncViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext("InitialSync", componentContext),
                    onSyncDone = ::hideSync,
                )
            )
        }

    suspend fun showSync() {
        log.debug { "show sync" }
        initialSyncNavigation.replaceCurrentSuspending(Config.Sync)
    }

    private fun hideSync() {
        log.debug { "hide sync" }
        initialSyncNavigation.launchReplaceCurrent(viewModelContext.coroutineScope, Config.None)
    }

    suspend fun close() {
        log.debug { "close initial sync view" }
        initialSyncNavigation.popSuspending()
    }

    sealed class Wrapper {
        data object None : Wrapper()
        data object Undefined : Wrapper()
        class Sync(val viewModel: SyncViewModel) : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object None : Config()

        @Serializable
        data object Undefined : Config()

        @Serializable
        data object Sync : Config()
    }
}
