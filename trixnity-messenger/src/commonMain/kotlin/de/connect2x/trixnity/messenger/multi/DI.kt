package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.util.platformCloseAppModule
import de.connect2x.trixnity.messenger.util.platformPathsModule
import de.connect2x.trixnity.messenger.util.platformSendLogToDevsModule
import de.connect2x.trixnity.messenger.util.platformUrlHandlerModule
import org.koin.dsl.module

fun createDefaultTrixnityMultiMessengerModules() = listOf(
    module {
        single<ProfileManager> {
            ProfileManagerImpl(get(), get(), get(), get())
        }
        single<CopyMultiMessengerSingletons> { CopyMultiMessengerSingletonsImpl() }
    },
    platformPathsModule(),
    platformMatrixMultiMessengerSettingsHolderModule(),
    matrixMessengerFactoryModule(),
    platformUrlHandlerModule(),
    platformCloseAppModule(),
    platformSendLogToDevsModule(),
    platformDeleteProfileDataModule()
)
