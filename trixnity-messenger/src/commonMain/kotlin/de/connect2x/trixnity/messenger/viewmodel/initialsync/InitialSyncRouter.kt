package de.connect2x.trixnity.messenger.viewmodel.initialsync

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.util.launchReplaceCurrent
import de.connect2x.trixnity.messenger.util.popSuspending
import de.connect2x.trixnity.messenger.util.replaceCurrentSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

class InitialSyncRouter(
    private val viewModelContext: ViewModelContext,
) {
    private val initialSyncNavigation = StackNavigation<InitialSyncConfig>()
    val stack = viewModelContext.childStack(
        source = initialSyncNavigation,
        serializer = InitialSyncConfig.serializer(),
        initialConfiguration = InitialSyncConfig.Undefined, // we do not yet know whether an initial sync is needed
        key = "initialSyncRouter",
        handleBackButton = false,
        childFactory = ::createInitialSyncChild,
    )

    private fun createInitialSyncChild(
        initialSyncConfig: InitialSyncConfig,
        componentContext: ComponentContext,
    ): InitialSyncWrapper =
        when (initialSyncConfig) {
            is InitialSyncConfig.None -> InitialSyncWrapper.None
            is InitialSyncConfig.Undefined -> InitialSyncWrapper.Undefined
            is InitialSyncConfig.Sync -> InitialSyncWrapper.Sync(
                viewModelContext.get<SyncViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onSyncDone = ::hideSync,
                )
            )
        }

    suspend fun showSync() {
        log.debug { "show sync" }
        initialSyncNavigation.replaceCurrentSuspending(InitialSyncConfig.Sync)
    }

    private fun hideSync() {
        log.debug { "hide sync" }
        initialSyncNavigation.launchReplaceCurrent(viewModelContext.coroutineScope, InitialSyncConfig.None)
    }

    suspend fun close() {
        log.debug { "close initial sync view" }
        initialSyncNavigation.popSuspending()
    }

    sealed class InitialSyncWrapper {
        data object None : InitialSyncWrapper()
        data object Undefined : InitialSyncWrapper()
        class Sync(val syncViewModel: SyncViewModel) : InitialSyncWrapper()
    }

    @Serializable
    sealed class InitialSyncConfig {
        @Serializable
        data object None : InitialSyncConfig()

        @Serializable
        data object Undefined : InitialSyncConfig()

        @Serializable
        data object Sync : InitialSyncConfig()
    }
}