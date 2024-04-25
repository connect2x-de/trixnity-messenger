package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.util.platformCloseAppModule
import de.connect2x.trixnity.messenger.util.platformDeleteProfileDataModule
import de.connect2x.trixnity.messenger.util.platformSendLogToDevsModule
import de.connect2x.trixnity.messenger.util.platformUrlHandlerModule
import org.koin.core.module.Module
import org.koin.dsl.module

fun createDefaultTrixnityMultiMessengerModules() = listOf(
    module {
        single<ProfileManager> {
            ProfileManagerImpl(get(), get(), get(), get())
        }
        single<CopyMultiMessengerSingletons> { CopyMultiMessengerSingletonsImpl() }
    },
    commonPlatformModule(),
    platformMatrixMultiMessengerSettingsHolderModule(),
    platformMatrixMessengerFactory(),
    platformUrlHandlerModule(),
    platformCloseAppModule(),
    platformSendLogToDevsModule(),
    platformDeleteProfileDataModule()
)

expect fun commonPlatformModule(): Module
