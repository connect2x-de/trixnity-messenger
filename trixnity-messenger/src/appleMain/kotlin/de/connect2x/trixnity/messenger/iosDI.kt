package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModelConfig
import org.koin.dsl.koinApplication
import org.koin.dsl.module

private val timelineViewModelConfigModule = module {
    single<TimelineViewModelConfig> {
        object : TimelineViewModelConfig {
            override val autoLoadBefore: Boolean = false
        }
    }
}

fun trixnityMessengerApplication() = koinApplication {
    modules(
        trixnityMessengerModule(),
        timelineViewModelConfigModule,
    )
}
