package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.platformModule
import de.connect2x.trixnity.messenger.util.SharedDataHandler
import de.connect2x.trixnity.messenger.util.SharedDataHandlerImpl
import de.connect2x.trixnity.messenger.util.platformCloseAppModule
import de.connect2x.trixnity.messenger.util.platformMinimizeAppModule
import de.connect2x.trixnity.messenger.util.platformPathsModule
import de.connect2x.trixnity.messenger.util.platformSendLogToDevsModule
import de.connect2x.trixnity.messenger.util.platformUriHandlerModule
import kotlinx.datetime.TimeZone
import net.folivo.trixnity.client.ModuleFactory
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock

fun createTrixnityMultiMessengerDefaultModuleFactories(): List<ModuleFactory> = listOf(
    {
        module {
            single<Clock> { Clock.System }
            single<TimeZone> { TimeZone.currentSystemDefault() }
            single<ProfileManager> {
                ProfileManagerImpl(get(), get(), get(), get())
            }
            single<CopyMultiMessengerSingletons>(named("DefaultCopyMultiMessengerSingletons")) {
                DefaultCopyMultiMessengerSingletons
            }
            single<SharedDataHandler> { SharedDataHandlerImpl() }
        }
    },
    ::platformModule,
    ::platformPathsModule,
    ::platformMatrixMultiMessengerSettingsHolderModule,
    ::matrixMessengerFactoryModule,
    ::platformUriHandlerModule,
    ::platformCloseAppModule,
    ::platformMinimizeAppModule,
    ::platformSendLogToDevsModule,
    ::platformDeleteProfileDataModule,
)
