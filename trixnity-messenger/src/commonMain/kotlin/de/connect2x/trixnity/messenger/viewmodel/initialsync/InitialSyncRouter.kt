package de.connect2x.trixnity.messenger.viewmodel.initialsync

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import de.connect2x.trixnity.messenger.util.launchReplaceCurrent
import de.connect2x.trixnity.messenger.util.popSuspending
import de.connect2x.trixnity.messenger.util.replaceCurrentSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

enum class InitialSyncState {
    NOT_DONE, DONE
}

class InitialSyncRouter(
    private val viewModelContext: ViewModelContext,
) {
    private val initialSyncNavigation = StackNavigation<InitialSyncConfig>()
    val stack = viewModelContext.childStack(
        source = initialSyncNavigation,
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
                    accountNames = initialSyncConfig.accountNames,
                    onSyncDone = ::hideSync,
                )
            )
        }

    suspend fun showSync(forAccounts: Map<String, InitialSyncState>) {
        log.debug { "show sync for the following accounts: $forAccounts" }
        initialSyncNavigation.replaceCurrentSuspending(InitialSyncConfig.Sync(forAccounts))
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
        object None : InitialSyncWrapper()
        object Undefined : InitialSyncWrapper()
        class Sync(val syncViewModel: SyncViewModel) : InitialSyncWrapper()
    }

    sealed class InitialSyncConfig : Parcelable {
        @Parcelize
        object None : InitialSyncConfig()

        @Parcelize
        object Undefined : InitialSyncConfig()

        @Parcelize
        data class Sync(val accountNames: Map<String, InitialSyncState>) : InitialSyncConfig()
    }
}