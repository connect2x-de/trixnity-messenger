package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncState
import de.connect2x.trixnity.messenger.viewmodel.initialsync.IosInitialSyncViewModel
import de.connect2x.trixnity.messenger.viewmodel.initialsync.SyncViewModel
import de.connect2x.trixnity.messenger.viewmodel.initialsync.SyncViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModelConfig
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.github.oshai.kotlinlogging.KotlinLoggingLevel
import org.koin.dsl.koinApplication
import org.koin.dsl.module

private val timelineViewModelConfigModule = module {
    single<TimelineViewModelConfig> {
        object : TimelineViewModelConfig {
            override val autoLoadBefore: Boolean = false
        }
    }
}

private val initialSyncModule = module {
    single<SyncViewModelFactory> {
        object : SyncViewModelFactory {
            override fun newSyncViewModel(
                viewModelContext: ViewModelContext,
                accountNames: Map<String, InitialSyncState>,
                onSyncDone: () -> Unit
            ): SyncViewModel {
                return IosInitialSyncViewModel(viewModelContext, onSyncDone)
            }
        }
    }
}

fun trixnityMessengerApplication() = koinApplication {
    modules(
        trixnityMessengerModule(),
        timelineViewModelConfigModule,
        initialSyncModule,
    )
}

fun setLoggingDebug() {
    KotlinLoggingConfiguration.logLevel = KotlinLoggingLevel.DEBUG
}